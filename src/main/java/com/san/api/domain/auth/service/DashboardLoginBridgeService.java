package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.DashboardBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DashboardLoginBridgeService {

    private static final Duration DASHBOARD_BRIDGE_TICKET_TTL = Duration.ofSeconds(30);

    private final LoginBridgeTicketService loginBridgeTicketService;
    private final TokenIssueService tokenIssueService;

    /**
     * 익스텐션 access token을 검증하고 대시보드 로그인에 사용할 일회성 bridge ticket을 발급합니다.
     */
    public LoginBridgeTicketResponse issueTicket(String accessToken) {
        return loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.EXTENSION,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET,
                DASHBOARD_BRIDGE_TICKET_TTL
        );
    }

    /**
     * 익스텐션에서 발급한 bridge ticket을 소비해 대시보드용 token pair를 발급합니다.
     */
    public TokenResponse exchangeToken(DashboardBridgeTokenRequest request) {
        String userId = loginBridgeTicketService.consumeTicket(
                request.ticket(),
                AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET
        );

        return tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD);
    }
}
