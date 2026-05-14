package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.request.SearchRequest;
import com.san.api.domain.knowledge.dto.response.SearchResponse;
import com.san.api.domain.knowledge.service.VectorSearchService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 자연어 통합 검색 API Controller */
@Tag(name = "Search", description = "자연어 통합 검색 API")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final VectorSearchService vectorSearchService;

    /**
     * 키워드 기반 지식카드 벡터 유사도 검색
     *
     * @param authentication 인증 정보
     * @param request        검색 조건 (keyword 필수, tag·fromDate·toDate 선택, page 기본값 0, size 기본값 10)
     * @return 검색 결과 및 페이지 정보
     */
    @Operation(summary = "지식카드 벡터 검색", description = "키워드를 벡터로 변환하여 유사 지식카드를 검색합니다. 태그·날짜 필터 지원.")
    @GetMapping
    public ApiResponse<SearchResponse> search(
            Authentication authentication,
            @Valid @ModelAttribute SearchRequest request
    ) {
        UUID userId = currentUserId(authentication);
        SearchResponse response = vectorSearchService.search(
                request.keyword(), userId, request.tag(), request.categoryId(),
                request.fromDate(), request.toDate(), request.page(), request.size());
        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
