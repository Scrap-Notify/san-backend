package com.san.api.global.logging;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 대해 메서드, URI, 처리 시간, 응답 상태를 INFO 레벨로 기록하는 필터.
 *
 * {@link OncePerRequestFilter}를 상속하여 요청 당 정확히 한 번만 실행됩니다.
 * 로그 형식: {@code [STATUS] METHOD URI - Xms}
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * 요청을 다음 필터 체인으로 전달하고, 완료 후 소요 시간과 응답 상태를 로깅합니다.
     * 예외 발생 시에도 {@code finally} 블록에서 반드시 로그를 기록합니다.
     *
     * @param request     현재 HTTP 요청
     * @param response    현재 HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     * @throws IOException      I/O 오류 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = resolveTraceId(request);
        response.setHeader(TRACE_ID_HEADER, traceId);
        AuditRequestContextHolder.set(new AuditRequestContext(
                traceId,
                resolveIpAddress(request),
                request.getHeader(USER_AGENT_HEADER)
        ));
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] {} {} - {}ms",
                    response.getStatus(),
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsed);
            AuditRequestContextHolder.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
