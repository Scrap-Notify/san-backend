package com.san.api.global.async.processor;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** resolveErrorMessage의 cause chain 추출 로직을 검증하는 테스트. */
class AsyncJobProcessorTest {

    /** 테스트용 최소 구현체 — process()는 사용하지 않음 */
    private final AsyncJobProcessor processor = (jobId, targetId) -> {};

    @Test
    void 일반_예외_단일_메시지를_ClassName_포맷으로_반환한다() {
        Exception exception = new IllegalArgumentException("잘못된 입력값");

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo("IllegalArgumentException: 잘못된 입력값");
    }

    @Test
    void BusinessException은_errorCode_getCode와_메시지를_조합해_반환한다() {
        Exception exception = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo("C005: 요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    void cause_chain이_있으면_caused_by로_연결해_반환한다() {
        Exception cause = new IllegalStateException("하위 원인");
        Exception exception = new RuntimeException("상위 메시지", cause);

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo(
                "RuntimeException: 상위 메시지\n  caused by: IllegalStateException: 하위 원인"
        );
    }

    @Test
    void cause_chain_중_BusinessException이_포함되면_해당_노드만_errorCode_포맷으로_반환한다() {
        Exception cause = new BusinessException(CommonErrorCode.UNAUTHORIZED);
        Exception exception = new RuntimeException("외부 래퍼 메시지", cause);

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo(
                "RuntimeException: 외부 래퍼 메시지\n  caused by: C003: 인증에 실패했습니다."
        );
    }

    @Test
    void getMessage가_null이면_해당_노드를_건너뛰고_하위_cause를_포함한다() {
        Exception cause = new IllegalStateException("하위 원인");
        Exception exception = new RuntimeException((String) null, cause);

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo("IllegalStateException: 하위 원인");
    }

    @Test
    void 모든_cause의_메시지가_null이면_fallback을_반환한다() {
        Exception cause = new RuntimeException((String) null);
        Exception exception = new RuntimeException((String) null, cause);

        String result = processor.resolveErrorMessage(exception, "기본 fallback 메시지");

        assertThat(result).isEqualTo("기본 fallback 메시지");
    }

    @Test
    void getMessage가_blank이면_해당_노드를_건너뛰고_하위_cause를_포함한다() {
        Exception cause = new IllegalStateException("실제 원인");
        Exception exception = new RuntimeException("   ", cause);

        String result = processor.resolveErrorMessage(exception, "fallback");

        assertThat(result).isEqualTo("IllegalStateException: 실제 원인");
    }
}
