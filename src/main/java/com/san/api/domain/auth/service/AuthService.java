package com.san.api.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.request.LoginRequest;
import com.san.api.domain.auth.dto.request.ReissueRequest;
import com.san.api.domain.auth.dto.request.SignupRequest;
import com.san.api.domain.auth.dto.request.WithdrawRequest;
import com.san.api.domain.auth.dto.response.SignupResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 인증 서비스.
 *
 * Redis 키 구조:
 * - {@code refresh:{userId}:{clientType}:{sessionId}}
 *   세션별 Refresh Token 메타데이터 JSON 저장
 *   (tokenHash, familyId, jti / TTL = refreshExpiration)
 * - {@code refresh:index:user:{userId}}
 *   회원 탈퇴, refresh token family 폐기 시 사용할 사용자별 refresh session key 목록
 * - {@code blacklist:{token}}
 *   로그아웃된 Access Token 저장
 *   (TTL = access token 남은 만료 시간)
 * - {@code fail:{username}}
 *   로그인 연속 실패 횟수 저장
 *   (TTL = failWindowSeconds)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final TokenIssueService tokenIssueService;
    private final AuthSessionKeyService authSessionKeyService;
    private final RefreshTokenHashService refreshTokenHashService;
    private final ObjectMapper objectMapper;

    @Value("${auth.login.max-fail-count}")
    private int maxFailCount;

    @Value("${auth.login.fail-window-seconds}")
    private long failWindowSeconds;

    @Value("${auth.login.lock-duration-seconds}")
    private long lockDurationSeconds;

    // ──────────────────────────── 아이디 중복 확인 ────────────────────────────

    @Transactional(readOnly = true)
    public void checkUsernameDuplicate(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }

    // ──────────────────────────── 회원가입 ────────────────────────────────────

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .build();

        return SignupResponse.from(userRepository.save(user));
    }

    // ──────────────────────────── 로그인 ──────────────────────────────────────

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        // 잠금 만료 자동 해제
        user.unlockIfExpired();
        if (user.isLocked()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleLoginFailure(user);
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        resetFailCount(user.getUsername());

        return tokenIssueService.issueTokenPair(user.getUserId().toString(), request.clientType());
    }

    // ──────────────────────────── Access Token 재발급 (Rotation) ──────────────

    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String userId = jwtProvider.getUserId(refreshToken);
        JwtSessionClaims sessionClaims = jwtProvider.getSessionClaims(refreshToken);
        String sessionId = requireSessionId(sessionClaims.sessionId(), AuthErrorCode.INVALID_REFRESH_TOKEN);
        String familyId = requireTokenClaim(sessionClaims.familyId(), AuthErrorCode.INVALID_REFRESH_TOKEN);
        String refreshKey = authSessionKeyService.refreshKey(userId, sessionClaims.clientType(), sessionId);
        RefreshTokenSession storedSession = deserializeSession(redisTemplate.opsForValue().getAndDelete(refreshKey));
        String requestedHash = refreshTokenHashService.hash(refreshToken);

        // Redis에는 refresh token 원문이 아니라 HMAC hash가 저장되므로 요청 토큰도 같은 방식으로 hash해서 비교합니다.
        // 값이 다르면 이미 rotation된 예전 refresh token이 다시 들어온 것으로 보고 재사용 탐지로 처리합니다.
        if (storedSession == null || !storedSession.tokenHash().equals(requestedHash)) {
            removeRefreshSessionIndex(userId, refreshKey);
            revokeRefreshFamily(userId, familyId, refreshKey);
            log.warn("[Auth] Refresh Token 재사용 감지 - userId={}, familyId={}", userId, familyId);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 정상 rotation은 같은 sessionId와 familyId를 유지한 채 refresh token만 새로 발급합니다.
        removeRefreshSessionIndex(userId, refreshKey);
        TokenResponse tokens = tokenIssueService.issueTokenPair(userId, sessionClaims.clientType(), sessionId, familyId);

        log.info("[Auth] 토큰 재발급 - userId={}", userId);
        return tokens;
    }

    public void logout(String accessToken) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = jwtProvider.getUserId(accessToken);
        JwtSessionClaims sessionClaims = jwtProvider.getSessionClaims(accessToken);
        String sessionId = requireSessionId(sessionClaims.sessionId(), AuthErrorCode.INVALID_ACCESS_TOKEN);

        deleteRefreshSession(userId, authSessionKeyService.refreshKey(userId, sessionClaims.clientType(), sessionId));

        long remainingMs = jwtProvider.getRemainingExpiration(accessToken);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    AuthRedisKeyPrefix.BLACKLIST + accessToken,
                    "1",
                    Duration.ofMillis(remainingMs)
            );
        }

        log.info("[Auth] 로그아웃 - userId={}", userId);
    }

    @Transactional
    public void withdraw(String userId, WithdrawRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        user.withdraw();
        deleteUserRefreshSessions(userId);

        log.info("[Auth] 회원탈퇴 - userId={}", userId);
    }

    private void handleLoginFailure(User user) {
        String failKey = AuthRedisKeyPrefix.LOGIN_FAIL + user.getUsername();
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1) {
            redisTemplate.expire(failKey, Duration.ofSeconds(failWindowSeconds));
        }

        log.warn("[Auth] 로그인 실패 - username={}, failCount={}", user.getUsername(), count);

        if (count != null && count >= maxFailCount) {
            LocalDateTime lockUntil = LocalDateTime.now().plusSeconds(lockDurationSeconds);
            user.lock(lockUntil);
            redisTemplate.delete(failKey);
            log.warn("[Auth] 계정 잠금 - username={}, lockedUntil={}", user.getUsername(), lockUntil);
        }
    }

    private void resetFailCount(String username) {
        redisTemplate.delete(AuthRedisKeyPrefix.LOGIN_FAIL + username);
    }

    private String requireSessionId(String sessionId, AuthErrorCode errorCode) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(errorCode);
        }
        return sessionId;
    }

    private String requireTokenClaim(String value, AuthErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorCode);
        }
        return value;
    }

    private void deleteUserRefreshSessions(String userId) {
        String indexKey = authSessionKeyService.userRefreshIndexKey(userId);
        // KEYS 명령 대신 사용자별 세션 인덱스 Set을 사용해 운영 환경의 Redis 블로킹 위험을 줄입니다.
        Set<String> keys = redisTemplate.opsForSet().members(indexKey);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(indexKey);
    }

    private void deleteRefreshSession(String userId, String refreshKey) {
        redisTemplate.delete(refreshKey);
        removeRefreshSessionIndex(userId, refreshKey);
    }

    private void removeRefreshSessionIndex(String userId, String refreshKey) {
        // 개별 세션 삭제 시 인덱스에서도 제거해 이후 전체 삭제/조회 대상에 남지 않게 합니다.
        redisTemplate.opsForSet().remove(authSessionKeyService.userRefreshIndexKey(userId), refreshKey);
    }

    private void revokeRefreshFamily(String userId, String familyId, String currentRefreshKey) {
        Set<String> keys = redisTemplate.opsForSet().members(authSessionKeyService.userRefreshIndexKey(userId));
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // 같은 familyId에서 나온 refresh token들을 모두 폐기해 탈취된 token 연쇄 사용을 막습니다.
        for (String key : keys) {
            if (key.equals(currentRefreshKey)) {
                continue;
            }
            RefreshTokenSession session = deserializeSession(redisTemplate.opsForValue().get(key));
            if (session != null && familyId.equals(session.familyId())) {
                deleteRefreshSession(userId, key);
            }
        }
    }

    private RefreshTokenSession deserializeSession(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, RefreshTokenSession.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
