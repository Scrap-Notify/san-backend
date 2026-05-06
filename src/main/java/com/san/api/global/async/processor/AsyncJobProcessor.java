package com.san.api.global.async.processor;

import com.san.api.global.exception.BusinessException;

import java.util.UUID;

/**
 * 비동기 잡 처리기 표준 인터페이스.
 *
 * 모든 비동기 작업(카드 분석, 리콜 생성 등) 구현체는 이 인터페이스를 반드시 구현해야 한다.
 */
public interface AsyncJobProcessor {

    void process(UUID jobId, UUID targetId);

    /**
     * 예외의 cause chain을 순회해 의미 있는 실패 메시지를 추출한다.
     * 전체 스택트레이스 대신 cause chain만 저장해 DB 부담을 줄인다.
     *
     * @param exception 발생 예외
     * @param fallback  cause chain이 모두 비어 있을 때 사용할 기본 메시지
     */
    default String resolveErrorMessage(Exception exception, String fallback) {
        StringBuilder sb = new StringBuilder();
        Throwable current = exception;

        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("\n  caused by: ");
                }
                if (current instanceof BusinessException be) {
                    sb.append(be.getErrorCode().getCode()).append(": ").append(message);
                } else {
                    sb.append(current.getClass().getSimpleName()).append(": ").append(message);
                }
            }
            current = current.getCause();
        }

        return sb.isEmpty() ? fallback : sb.toString();
    }
}
