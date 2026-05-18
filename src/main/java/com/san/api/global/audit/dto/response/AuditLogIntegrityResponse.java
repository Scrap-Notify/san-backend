package com.san.api.global.audit.dto.response;

import com.san.api.global.audit.entity.AuditIntegrityStatus;

import java.time.Instant;
import java.util.UUID;

public record AuditLogIntegrityResponse(
        UUID auditLogEventId,
        AuditIntegrityStatus status,
        boolean valid,
        Instant verifiedAt
) {
}
