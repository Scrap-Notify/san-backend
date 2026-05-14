package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.ExtensionBridgeTokenRequest;
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
class ExtensionLoginBridgeServiceTest {

    @Mock
    private LoginBridgeTicketService loginBridgeTicketService;

    @Mock
    private TokenIssueService tokenIssueService;

    private ExtensionLoginBridgeService extensionLoginBridgeService;

    @BeforeEach
    void setUp() {
        extensionLoginBridgeService = new ExtensionLoginBridgeService(loginBridgeTicketService, tokenIssueService);
    }

    @Test
    void issueTicketDelegatesDashboardToExtensionBridge() {
        String accessToken = "dashboard-access-token";
        LoginBridgeTicketResponse ticketResponse = LoginBridgeTicketResponse.of("bridge-ticket", 120L);

        when(loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.DASHBOARD,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET,
                Duration.ofMinutes(2)
        )).thenReturn(ticketResponse);

        LoginBridgeTicketResponse result = extensionLoginBridgeService.issueTicket(accessToken);

        assertThat(result).isEqualTo(ticketResponse);
    }

    @Test
    void exchangeTokenConsumesExtensionTicketAndIssuesExtensionTokenPair() {
        String userId = UUID.randomUUID().toString();
        String ticket = "bridge-ticket";
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800L, "extension-session");

        when(loginBridgeTicketService.consumeTicket(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET))
                .thenReturn(userId);
        when(tokenIssueService.issueTokenPair(userId, ClientType.EXTENSION)).thenReturn(tokenResponse);

        TokenResponse result = extensionLoginBridgeService.exchangeToken(new ExtensionBridgeTokenRequest(ticket));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.EXTENSION);
    }
}
