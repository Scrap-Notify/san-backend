package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.LoginBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class LoginBridgeService {

    private static final Duration BRIDGE_TICKET_TTL = Duration.ofMinutes(2);

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final TokenIssueService tokenIssueService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Dashboard access token을 검증하고 Extension 로그인을 위한 일회용 bridge ticket을 발급합니다.
     *
     * @param accessToken 현재 Dashboard 요청의 Bearer access token
     * @return Extension에서 token pair로 교환할 수 있는 일회용 ticket
     */
    public LoginBridgeTicketResponse issueTicket(String accessToken) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        if (jwtProvider.getSessionClaims(accessToken).clientType() != ClientType.DASHBOARD) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String ticket = generateUrlSafeToken();
        redisTemplate.opsForValue().set(
                AuthRedisKeyPrefix.LOGIN_BRIDGE_TICKET + ticket,
                jwtProvider.getUserId(accessToken),
                BRIDGE_TICKET_TTL
        );

        return LoginBridgeTicketResponse.of(ticket, BRIDGE_TICKET_TTL.toSeconds());
    }

    /**
     * 일회용 bridge ticket을 Extension용 서비스 JWT token pair로 교환합니다.
     *
     * @param request Dashboard에서 발급받은 일회용 bridge ticket
     * @return Extension 클라이언트 유형으로 발급된 access/refresh token pair
     */
    public TokenResponse exchangeToken(LoginBridgeTokenRequest request) {
        String key = AuthRedisKeyPrefix.LOGIN_BRIDGE_TICKET + request.ticket();
        String userId = redisTemplate.opsForValue().getAndDelete(key);
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET);
        }

        return tokenIssueService.issueTokenPair(userId, ClientType.EXTENSION);
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
