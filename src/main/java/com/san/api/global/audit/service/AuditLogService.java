package com.san.api.global.audit.service;

import com.san.api.domain.user.entity.User;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 감사 로그 이벤트 저장 서비스.
 * 각 도메인의 사용자 주요 행위와 외부 연동 처리 결과를 공통 방식으로 audit_log_events 테이블에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final EntityManager entityManager;
    private final AuditLogEventRepository auditLogEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID save(AuditLogCreateCommand command) {
        AuditLogEvent event = AuditLogEvent.builder()
                .actorUser(getActorUserReference(command.actorUserId()))
                .traceId(command.traceId())
                .eventDomain(command.eventDomain())
                .eventType(command.eventType())
                .targetType(command.targetType())
                .targetId(command.targetId())
                .outcome(command.outcome())
                .failureReasonCode(command.failureReasonCode())
                .failureMessage(command.failureMessage())
                .ipAddress(command.ipAddress())
                .userAgent(command.userAgent())
                .metadata(command.metadata())
                .build();

        return auditLogEventRepository.save(event).getAuditLogEventId();
    }

    private User getActorUserReference(UUID actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        return entityManager.getReference(User.class, actorUserId);
    }
}
