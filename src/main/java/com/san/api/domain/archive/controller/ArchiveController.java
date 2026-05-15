package com.san.api.domain.archive.controller;

import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.archive.service.ArchiveService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 아카이브 API Controller */
@Tag(name = "Archive", description = "아카이브 API")
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 아카이브 카테고리 목록 조회
     *
     * @param authentication 인증 정보
     * @return 카테고리별 지식카드 개수 응답
     */
    @Operation(summary = "아카이브 카테고리 조회", description = "지식나무 초기 화면에 사용할 카테고리 목록과 카테고리별 지식카드 개수를 조회")
    @GetMapping("/categories")
    public ApiResponse<ArchiveCategoryListResponse> getCategories(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
