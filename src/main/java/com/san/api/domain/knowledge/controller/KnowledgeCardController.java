package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisJobResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisResultResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardListResponse;
import com.san.api.domain.knowledge.service.KnowledgeCardService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 지식카드 API Controller */
@Tag(name = "KnowledgeCard", description = "지식카드 API")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class KnowledgeCardController {

    private final KnowledgeCardService knowledgeCardService;

    /**
     * 저장된 수집 원본 기반 지식카드 AI 분석 작업 등록
     *
     * @param authentication 인증 정보
     * @param request 지식카드 AI 분석 요청
     * @return 등록된 비동기 작업 응답
     */
    @Operation(summary = "지식카드 AI 분석 작업 등록", description = "저장된 수집 원본을 지식카드로 분석하는 비동기 작업을 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<KnowledgeCardAnalysisJobResponse> createCard(
            Authentication authentication,
            @Valid @RequestBody KnowledgeCardCreateRequest request) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardAnalysisJobResponse response = knowledgeCardService.createCard(userId, request);

        return ApiResponse.success(response);
    }

    /**
     * 지식카드 AI 분석 작업 결과 조회
     *
     * @param authentication 인증 정보
     * @param jobId 분석 작업 ID
     * @return 지식카드 AI 분석 작업 결과
     */
    @Operation(summary = "지식카드 AI 분석 작업 결과 조회", description = "작업 ID로 지식카드 AI 분석 상태와 생성된 지식카드를 조회")
    @GetMapping("/jobs/{jobId}/result")
    public ApiResponse<KnowledgeCardAnalysisResultResponse> getAnalysisResult(
            Authentication authentication,
            @PathVariable UUID jobId) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardAnalysisResultResponse response = knowledgeCardService.getAnalysisResult(userId, jobId);

        return ApiResponse.success(response);
    }

    /**
     * 로그인 사용자 기준 지식카드 목록 조회
     *
     * @param authentication 인증 정보
     * @return 지식카드 목록 응답
     */
    @Operation(summary = "지식카드 목록 조회", description = "로그인 사용자의 지식카드 목록을 최신순으로 조회")
    @GetMapping
    public ApiResponse<KnowledgeCardListResponse> getCards(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        KnowledgeCardListResponse response = knowledgeCardService.getCards(userId);

        return ApiResponse.success(response);
    }

    /**
     * 인증 정보에서 사용자 ID 추출
     *
     * @param authentication 인증 정보
     * @return 로그인 사용자 ID
     */
    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
