package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.DashboardBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardLoginBridgeServiceTest {

    @Mock
    private LoginBridgeTicketService loginBridgeTicketService;

    @Mock
    private TokenIssueService tokenIssueService;

    private DashboardLoginBridgeService dashboardLoginBridgeService;

    @BeforeEach
    void setUp() {
        dashboardLoginBridgeService = new DashboardLoginBridgeService(loginBridgeTicketService, tokenIssueService);
    }

    @Test
    void issueTicketDelegatesExtensionToDashboardBridge() {
        String accessToken = "extension-access-token";
        LoginBridgeTicketResponse ticketResponse = LoginBridgeTicketResponse.of("dashboard-ticket", 30L);

        when(loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.EXTENSION,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET,
                Duration.ofSeconds(30)
        )).thenReturn(ticketResponse);

        LoginBridgeTicketResponse result = dashboardLoginBridgeService.issueTicket(accessToken);

        assertThat(result).isEqualTo(ticketResponse);
    }

    @Test
    void exchangeTokenConsumesDashboardTicketAndIssuesDashboardTokenPair() {
        String userId = UUID.randomUUID().toString();
        String ticket = "dashboard-ticket";
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800L, "dashboard-session");

        when(loginBridgeTicketService.consumeTicket(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET))
                .thenReturn(userId);
        when(tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD)).thenReturn(tokenResponse);

        TokenResponse result = dashboardLoginBridgeService.exchangeToken(new DashboardBridgeTokenRequest(ticket));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.DASHBOARD);
    }
}
