package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.response.SearchResponse;
import com.san.api.domain.knowledge.service.VectorSearchService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** 자연어 통합 검색 API Controller */
@Tag(name = "Search", description = "자연어 통합 검색 API")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final VectorSearchService vectorSearchService;

    /**
     * 키워드 기반 지식카드 벡터 유사도 검색
     *
     * @param authentication 인증 정보
     * @param keyword        검색어 (필수)
     * @param tag            태그명 필터 (선택)
     * @param fromDate       카드 생성일 시작 범위 (선택, yyyy-MM-dd)
     * @param toDate         카드 생성일 종료 범위 (선택, yyyy-MM-dd)
     * @param page           페이지 번호 (기본값 0)
     * @param size           페이지 크기 (기본값 10)
     * @return 검색 결과 및 페이지 정보
     */
    @Operation(summary = "지식카드 벡터 검색", description = "키워드를 벡터로 변환하여 유사 지식카드를 검색합니다. 태그·날짜 필터 지원.")
    @GetMapping
    public ApiResponse<SearchResponse> search(
            Authentication authentication,
            @RequestParam String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = currentUserId(authentication);
        SearchResponse response = vectorSearchService.search(keyword, userId, tag, fromDate, toDate, page, size);
        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
