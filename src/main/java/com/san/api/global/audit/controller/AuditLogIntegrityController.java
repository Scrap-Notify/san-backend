package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.response.AuditLogIntegrityResponse;
import com.san.api.global.audit.dto.response.AuditLogIntegritySummaryResponse;
import com.san.api.global.audit.service.AuditLogIntegrityService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Audit Log Integrity", description = "감사 로그 무결성 검증 API")
@Validated
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogIntegrityController {

    private final AuditLogIntegrityService auditLogIntegrityService;

    @Operation(summary = "감사 로그 단건 무결성 검증")
    @GetMapping("/{auditLogEventId}/integrity")
    public ApiResponse<AuditLogIntegrityResponse> verify(@PathVariable UUID auditLogEventId) {
        return ApiResponse.success(auditLogIntegrityService.verify(auditLogEventId));
    }

    @Operation(summary = "감사 로그 기간 무결성 검증")
    @GetMapping("/integrity")
    public ApiResponse<AuditLogIntegritySummaryResponse> verifyRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        return ApiResponse.success(auditLogIntegrityService.verifyRange(from, to, page, size));
    }
}
