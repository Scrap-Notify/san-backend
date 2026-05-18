package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.service.AuditLogQueryService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Audit Log", description = "감사 로그 API")
@Validated
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @Operation(summary = "감사 로그 조회", description = "감사 로그를 조건별로 최신순 조회합니다.")
    @GetMapping
    public ApiResponse<AuditLogPageResponse> search(
            Authentication authentication,
            @ModelAttribute AuditLogSearchRequest request,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(auditLogQueryService.search(
                request.forActor(currentUserId(authentication)),
                page,
                size
        ));
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
