package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.LoginRequest;
import com.san.api.domain.auth.dto.request.ReissueRequest;
import com.san.api.domain.auth.dto.request.WithdrawRequest;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 일반 인증 서비스의 클라이언트별 세션 처리 동작을 검증합니다. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private TokenIssueService tokenIssueService;

    private AuthSessionKeyService authSessionKeyService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authSessionKeyService = new AuthSessionKeyService();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtProvider,
                redisTemplate,
                tokenIssueService,
                authSessionKeyService
        );
        ReflectionTestUtils.setField(authService, "maxFailCount", 5);
        ReflectionTestUtils.setField(authService, "failWindowSeconds", 300L);
        ReflectionTestUtils.setField(authService, "lockDurationSeconds", 600L);
    }

    @Test
    void loginIssuesTokenPairForRequestedClientType() {
        User user = localUser();
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800, "session-id");
        when(userRepository.findByUsername("dahyeon")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(tokenIssueService.issueTokenPair(user.getUserId().toString(), ClientType.EXTENSION))
                .thenReturn(tokenResponse);

        TokenResponse result = authService.login(new LoginRequest("dahyeon", "password", ClientType.EXTENSION));

        assertThat(result).isEqualTo(tokenResponse);
        verify(redisTemplate).delete(AuthRedisKeyPrefix.LOGIN_FAIL + "dahyeon");
    }

    @Test
    void reissueValidatesRefreshTokenByClientSessionKey() {
        String userId = UUID.randomUUID().toString();
        String sessionId = "dashboard-session";
        String refreshToken = "refresh-token";
        String redisKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId;
        TokenResponse tokenResponse = TokenResponse.of("new-access-token", "new-refresh-token", 1800, sessionId);

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(refreshToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, sessionId));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get(redisKey)).thenReturn(refreshToken);
        when(tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD, sessionId))
                .thenReturn(tokenResponse);

        TokenResponse result = authService.reissue(new ReissueRequest(refreshToken));

        assertThat(result).isEqualTo(tokenResponse);
        verify(redisTemplate).delete(redisKey);
        verify(setOperations).remove(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId, redisKey);
    }

    @Test
    void logoutDeletesOnlyCurrentClientSessionRefreshToken() {
        String userId = UUID.randomUUID().toString();
        String sessionId = "extension-session";
        String accessToken = "access-token";
        String redisKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:" + sessionId;

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.EXTENSION, sessionId));
        when(jwtProvider.getRemainingExpiration(accessToken)).thenReturn(1000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        authService.logout(accessToken);

        verify(redisTemplate).delete(redisKey);
        verify(setOperations).remove(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId, redisKey);
        verify(valueOperations).set(AuthRedisKeyPrefix.BLACKLIST + accessToken, "1", Duration.ofMillis(1000L));
    }

    @Test
    void withdrawDeletesAllRefreshSessionsForUser() {
        User user = localUser();
        String userId = user.getUserId().toString();
        Set<String> sessionKeys = Set.of(
                AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:session-a",
                AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:session-b"
        );

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId)).thenReturn(sessionKeys);

        authService.withdraw(userId, new WithdrawRequest("password"));

        verify(redisTemplate).delete(sessionKeys);
        verify(redisTemplate).delete(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId);
    }

    private User localUser() {
        return User.builder()
                .username("dahyeon")
                .passwordHash("encoded-password")
                .provider(AuthProvider.LOCAL)
                .build();
    }
}
