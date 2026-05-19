package com.san.api.global.outbox.repository;

import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Outbox 이벤트의 저장과 조회를 담당하는 Repository입니다. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * 처리 가능한 대기 이벤트를 생성 순서대로 최대 100건 조회합니다.
     *
     * @param status 조회할 이벤트 상태
     * @param now    현재 시각
     * @return 처리 가능한 Outbox 이벤트 목록
     */
    List<OutboxEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status,
            LocalDateTime now
    );

    /**
     * 운영 조회를 위해 특정 상태의 이벤트를 최신순으로 조회합니다.
     *
     * @param status 조회할 이벤트 상태
     * @return 해당 상태의 Outbox 이벤트 목록
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxEventStatus status);
}
