package com.san.api.domain.auth.service;

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
import java.util.UUID;

/**
 * 인증 서비스.
 *
 * Redis 키 구조:
 * - {@code refresh:{userId}}   — 유효한 Refresh Token 값 (TTL = refreshExpiration)
 * - {@code blacklist:{token}}  — 로그아웃된 Access Token (TTL = 토큰 남은 만료시간)
 * - {@code fail:{username}}    — 로그인 연속 실패 횟수 (TTL = failWindowSeconds)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtProvider        jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final TokenIssueService tokenIssueService;

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

        return tokenIssueService.issueTokenPair(user.getUserId().toString());
    }

    // ──────────────────────────── Access Token 재발급 (Rotation) ──────────────

    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtProvider.validateToken(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String userId    = jwtProvider.getUserId(refreshToken);
        String redisKey  = AuthRedisKeyPrefix.REFRESH + userId;
        String stored    = redisTemplate.opsForValue().get(redisKey);

        // Redis에 없거나 값이 다르면 → 탈취 감지 or 만료
        if (stored == null || !stored.equals(refreshToken)) {
            // 재사용 감지 시 해당 사용자 세션 전체 무효화
            redisTemplate.delete(redisKey);
            log.warn("[Auth] Refresh Token 재사용 감지 - userId={}", userId);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 기존 토큰 삭제 후 새 토큰 발급 (Rotation)
        redisTemplate.delete(redisKey);
        TokenResponse tokens = tokenIssueService.issueTokenPair(userId);

        log.info("[Auth] 토큰 재발급 - userId={}", userId);
        return tokens;
    }

    // ──────────────────────────── 로그아웃 ────────────────────────────────────

    public void logout(String accessToken) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = jwtProvider.getUserId(accessToken);

        // Refresh Token 삭제
        redisTemplate.delete(AuthRedisKeyPrefix.REFRESH + userId);

        // Access Token 블랙리스트 등록 (남은 유효시간만큼 TTL)
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

    // ──────────────────────────── 회원탈퇴 ────────────────────────────────────

    @Transactional
    public void withdraw(String userId, WithdrawRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        user.withdraw();

        // 세션 완전 정리
        redisTemplate.delete(AuthRedisKeyPrefix.REFRESH + userId);

        log.info("[Auth] 회원탈퇴 - userId={}", userId);
    }

    // ──────────────────────────── 내부 헬퍼 ───────────────────────────────────

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
}
