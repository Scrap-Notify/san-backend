package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.response.AuditLogIntegrityResponse;
import com.san.api.global.audit.dto.response.AuditLogIntegritySummaryResponse;
import com.san.api.global.audit.entity.AuditIntegrityStatus;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogIntegrityService {

    private static final int MAX_VERIFY_SIZE = 500;

    private final AuditLogEventRepository auditLogEventRepository;
    private final AuditLogIntegrityHasher auditLogIntegrityHasher;

    @Transactional(readOnly = true)
    public AuditLogIntegrityResponse verify(UUID auditLogEventId) {
        AuditLogEvent event = auditLogEventRepository.findById(auditLogEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        AuditIntegrityStatus status = status(event);
        return new AuditLogIntegrityResponse(
                event.getAuditLogEventId(),
                status,
                status == AuditIntegrityStatus.VALID,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public AuditLogIntegritySummaryResponse verifyRange(LocalDateTime from, LocalDateTime to, int page, int size) {
        if (from.isAfter(to)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_VERIFY_SIZE);
        List<AuditLogIntegrityResponse> results = auditLogEventRepository.findAll(
                        occurredBetween(from, to),
                        PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "occurredAt"))
                )
                .map(event -> {
                    AuditIntegrityStatus status = status(event);
                    return new AuditLogIntegrityResponse(
                            event.getAuditLogEventId(),
                            status,
                            status == AuditIntegrityStatus.VALID,
                            Instant.now()
                    );
                })
                .getContent();

        List<UUID> invalidEventIds = results.stream()
                .filter(result -> result.status() == AuditIntegrityStatus.INVALID)
                .map(AuditLogIntegrityResponse::auditLogEventId)
                .toList();
        List<UUID> missingHashEventIds = results.stream()
                .filter(result -> result.status() == AuditIntegrityStatus.MISSING_HASH)
                .map(AuditLogIntegrityResponse::auditLogEventId)
                .toList();

        return new AuditLogIntegritySummaryResponse(
                results.size(),
                (int) results.stream().filter(AuditLogIntegrityResponse::valid).count(),
                invalidEventIds.size(),
                missingHashEventIds.size(),
                invalidEventIds,
                missingHashEventIds,
                Instant.now()
        );
    }

    private AuditIntegrityStatus status(AuditLogEvent event) {
        if (event.getIntegrityHash() == null || event.getIntegrityHash().isBlank()) {
            return AuditIntegrityStatus.MISSING_HASH;
        }
        String calculatedHash = auditLogIntegrityHasher.hash(event);
        if (event.getIntegrityHash().equals(calculatedHash)) {
            return AuditIntegrityStatus.VALID;
        }
        return AuditIntegrityStatus.INVALID;
    }

    private Specification<AuditLogEvent> occurredBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from),
                criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), to)
        );
    }
}
