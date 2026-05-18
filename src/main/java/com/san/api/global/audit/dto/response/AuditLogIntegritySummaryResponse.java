package com.san.api.global.audit.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditLogIntegritySummaryResponse(
        int checkedCount,
        int validCount,
        int invalidCount,
        int missingHashCount,
        List<UUID> invalidEventIds,
        List<UUID> missingHashEventIds,
        Instant verifiedAt
) {
}
