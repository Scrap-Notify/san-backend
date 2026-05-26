package com.san.api.global.audit.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.dto.AuditRecordCommand;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.exception.errorcode.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRecorder {

    private final AuditLogService auditLogService;

    public UUID recordSuccess(AuditRecordCommand command) {
        return auditLogService.save(toCreateCommand(command, AuditOutcome.SUCCESS, null, null, command.metadata()));
    }

    public UUID recordFailure(AuditRecordCommand command, String failureReasonCode, String failureMessage) {
        return auditLogService.save(toCreateCommand(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode,
                failureMessage,
                command.metadata()
        ));
    }

    public UUID recordFailure(AuditRecordCommand command, ErrorCode errorCode) {
        return auditLogService.save(toCreateCommand(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode(errorCode),
                errorCode == null ? null : errorCode.getMessage(),
                failureMetadata(command.metadata(), errorCode)
        ));
    }

    public Optional<UUID> recordSuccessSafely(AuditRecordCommand command) {
        return saveSafely(command, AuditOutcome.SUCCESS, null, null, command.metadata());
    }

    public Optional<UUID> recordFailureSafely(
            AuditRecordCommand command,
            String failureReasonCode,
            String failureMessage
    ) {
        return saveSafely(command, AuditOutcome.FAILURE, failureReasonCode, failureMessage, command.metadata());
    }

    public Optional<UUID> recordFailureSafely(AuditRecordCommand command, ErrorCode errorCode) {
        return saveSafely(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode(errorCode),
                errorCode == null ? null : errorCode.getMessage(),
                failureMetadata(command.metadata(), errorCode)
        );
    }

    private Optional<UUID> saveSafely(
            AuditRecordCommand command,
            AuditOutcome outcome,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        try {
            return Optional.of(auditLogService.save(toCreateCommand(
                    command,
                    outcome,
                    failureReasonCode,
                    failureMessage,
                    metadata
            )));
        } catch (RuntimeException e) {
            log.warn(
                    "감사 로그 저장 실패 - eventDomain={}, eventType={}, outcome={}, actorUserId={}",
                    command.eventDomain(),
                    command.eventType(),
                    outcome,
                    command.actorUserId(),
                    e
            );
            return Optional.empty();
        }
    }

    private AuditLogCreateCommand toCreateCommand(
            AuditRecordCommand command,
            AuditOutcome outcome,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        return new AuditLogCreateCommand(
                command.actorUserId(),
                firstNonBlank(command.traceId(), context == null ? null : context.traceId()),
                command.eventDomain(),
                command.eventType(),
                command.targetType(),
                command.targetId(),
                outcome,
                failureReasonCode,
                failureMessage,
                firstNonBlank(command.ipAddress(), context == null ? null : context.ipAddress()),
                firstNonBlank(command.userAgent(), context == null ? null : context.userAgent()),
                metadata
        );
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String failureReasonCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return "UNKNOWN_FAILURE";
        }
        return errorCode.getCode();
    }

    private Map<String, Object> failureMetadata(Map<String, Object> metadata, ErrorCode errorCode) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (errorCode != null) {
            merged.put("clientErrorCode", errorCode.getCode());
            merged.put("httpStatus", errorCode.getStatus().value());
        }
        return merged;
    }
}
