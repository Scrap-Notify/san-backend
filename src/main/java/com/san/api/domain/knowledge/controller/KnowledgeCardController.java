package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
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
     * 저장된 수집 원본 기반 지식카드 생성
     *
     * @param authentication 인증 정보
     * @param request 지식카드 생성 요청
     * @return 생성된 지식카드 응답
     */
    @Operation(summary = "지식카드 생성", description = "저장된 수집 원본을 AI 분석해 지식카드로 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<KnowledgeCardResponse> createCard(
            Authentication authentication,
            @Valid @RequestBody KnowledgeCardCreateRequest request) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardResponse response = knowledgeCardService.createCard(userId, request);

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
