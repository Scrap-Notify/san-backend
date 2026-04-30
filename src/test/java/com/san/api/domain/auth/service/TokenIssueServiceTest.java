package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 서비스 JWT 토큰 발급과 refresh token Redis 저장을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class TokenIssueServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenIssueService tokenIssueService;

    @BeforeEach
    void setUp() {
        tokenIssueService = new TokenIssueService(jwtProvider, redisTemplate);
        ReflectionTestUtils.setField(tokenIssueService, "accessExpiration", 1800000L);
        ReflectionTestUtils.setField(tokenIssueService, "refreshExpiration", 604800000L);
    }

    @Test
    void issueTokenPairStoresRefreshTokenWithTtl() {
        String userId = "user-id";
        when(jwtProvider.generateAccessToken(userId)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(userId)).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenResponse response = tokenIssueService.issueTokenPair(userId);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        verify(valueOperations).set(
                AuthRedisKeyPrefix.REFRESH + userId,
                "refresh-token",
                Duration.ofMillis(604800000L)
        );
    }
}
