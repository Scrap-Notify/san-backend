package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.LoginBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginBridgeService의 Dashboard/Extension 로그인 브릿지 ticket 발급과 token 교환을 검증합니다.
 *
 * Dashboard access token만 bridge ticket을 발급할 수 있고, ticket 교환 시 Extension clientType의
 * token pair가 발급되며 ticket은 1회 사용 후 삭제되는지 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class LoginBridgeServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TokenIssueService tokenIssueService;

    private LoginBridgeService loginBridgeService;

    @BeforeEach
    void setUp() {
        loginBridgeService = new LoginBridgeService(jwtProvider, redisTemplate, tokenIssueService);
    }

    @Test
    void issueTicketStoresUserIdForDashboardAccessToken() {
        String userId = UUID.randomUUID().toString();
        String accessToken = "dashboard-access-token";

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, "dashboard-session", null, null));
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginBridgeTicketResponse response = loginBridgeService.issueTicket(accessToken);

        assertThat(response.ticket()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(120L);
        verify(valueOperations).set(
                AuthRedisKeyPrefix.LOGIN_BRIDGE_TICKET + response.ticket(),
                userId,
                Duration.ofMinutes(2)
        );
    }

    @Test
    void issueTicketRejectsExtensionAccessToken() {
        String accessToken = "extension-access-token";

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.EXTENSION, "extension-session", null, null));

        assertThatThrownBy(() -> loginBridgeService.issueTicket(accessToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_ACCESS_TOKEN.getMessage());
    }

    @Test
    void exchangeTokenIssuesExtensionTokenPairAndDeletesTicket() {
        String userId = UUID.randomUUID().toString();
        String ticket = "bridge-ticket";
        String key = AuthRedisKeyPrefix.LOGIN_BRIDGE_TICKET + ticket;
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800L, "extension-session");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(key)).thenReturn(userId);
        when(tokenIssueService.issueTokenPair(userId, ClientType.EXTENSION)).thenReturn(tokenResponse);

        TokenResponse result = loginBridgeService.exchangeToken(new LoginBridgeTokenRequest(ticket));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.EXTENSION);
    }

    @Test
    void exchangeTokenRejectsMissingTicket() {
        String ticket = "missing-ticket";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(AuthRedisKeyPrefix.LOGIN_BRIDGE_TICKET + ticket)).thenReturn(null);

        assertThatThrownBy(() -> loginBridgeService.exchangeToken(new LoginBridgeTokenRequest(ticket)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET.getMessage());
    }
}
