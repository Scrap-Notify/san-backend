package com.san.api.global.audit.dto.request;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogSearchRequest(
        UUID actorUserId,
        String traceId,
        AuditEventDomain eventDomain,
        AuditEventType eventType,
        AuditOutcome outcome,
        String targetType,
        UUID targetId,
        String failureReasonCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to
) {
}
