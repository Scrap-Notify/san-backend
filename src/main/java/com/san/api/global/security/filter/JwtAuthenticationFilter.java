package com.san.api.global.security.filter;

import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.ErrorCode;
import com.san.api.global.security.handler.SecurityErrorAttribute;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import com.san.api.global.security.token.BearerTokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization 헤더의 Bearer access token을 검증하고 인증 정보를 등록합니다.
 *
 * <p>토큰이 유효하고 blacklist에 없으면 {@code SecurityContext}에 인증 정보를
 * 저장합니다. 토큰이 잘못되었거나 로그아웃되어 blacklist에 있으면 직접 응답하지
 * 않고 request attribute에 실패 사유만 남깁니다. 최종 오류 응답은
 * {@code CustomAuthenticationEntryPoint}가 공통 형식으로 작성합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    /**
     * 요청마다 한 번 실행되어 Bearer access token을 인증 처리합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 필터 처리 실패 시
     * @throws IOException 필터 처리 중 입출력 실패 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenResolver.resolve(request);

        if (token != null) {
            if (!jwtProvider.validateToken(token) || !jwtProvider.isAccessToken(token)) {
                setSecurityError(request, AuthErrorCode.INVALID_ACCESS_TOKEN);
            } else if (isBlacklisted(token)) {
                setSecurityError(request, AuthErrorCode.TOKEN_BLACKLISTED);
            } else {
                Authentication auth = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 로그아웃 처리된 access token인지 Redis blacklist에서 확인합니다.
     *
     * @param token 검사할 access token
     * @return blacklist에 존재하면 true
     */
    private boolean isBlacklisted(String token) {
        return redisTemplate.opsForValue().get(AuthRedisKeyPrefix.BLACKLIST + token) != null;
    }

    /**
     * Security Handler가 사용할 인증 실패 사유를 request attribute에 저장합니다.
     *
     * @param request 현재 요청
     * @param errorCode 클라이언트에 내려줄 인증 오류 코드
     */
    private void setSecurityError(HttpServletRequest request, ErrorCode errorCode) {
        request.setAttribute(SecurityErrorAttribute.ERROR_CODE, errorCode);
    }
}
