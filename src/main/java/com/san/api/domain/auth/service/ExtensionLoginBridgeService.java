package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.ExtensionBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ExtensionLoginBridgeService {

    private static final Duration EXTENSION_BRIDGE_TICKET_TTL = Duration.ofMinutes(2);

    private final LoginBridgeTicketService loginBridgeTicketService;
    private final TokenIssueService tokenIssueService;

    /**
     * 대시보드 access token을 검증하고 익스텐션 로그인에 사용할 일회성 bridge ticket을 발급합니다.
     */
    public LoginBridgeTicketResponse issueTicket(String accessToken) {
        return loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.DASHBOARD,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET,
                EXTENSION_BRIDGE_TICKET_TTL
        );
    }

    /**
     * 대시보드에서 발급한 bridge ticket을 소비해 익스텐션용 token pair를 발급합니다.
     */
    public TokenResponse exchangeToken(ExtensionBridgeTokenRequest request) {
        String userId = loginBridgeTicketService.consumeTicket(
                request.ticket(),
                AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET
        );

        return tokenIssueService.issueTokenPair(userId, ClientType.EXTENSION);
    }
}
