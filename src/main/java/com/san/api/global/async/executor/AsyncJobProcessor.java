package com.san.api.global.async.executor;

import java.util.UUID;

/**
 * 비동기 잡 처리기 표준 인터페이스.
 *
 * 모든 비동기 작업(카드 분석, 리콜 생성 등) 구현체는 이 인터페이스를 반드시 구현해야 한다.
 */
public interface AsyncJobProcessor {

    void process(UUID jobId, UUID targetId);
}
