package com.san.api.domain.recall.controller;

import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.dto.response.RecallQuizGenerateResponse;
import com.san.api.domain.recall.service.RecallQuizGenerationService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 리콜 API Controller */
@Tag(name = "Recall", description = "리콜 API")
@RestController
@RequestMapping("/recall")
@RequiredArgsConstructor
public class RecallController {

    private final RecallQuizGenerationService recallQuizGenerationService;

    /**
     * 날짜 기반 리콜 퀴즈 생성
     *
     * @param authentication 인증 정보
     * @param request 리콜 퀴즈 생성 요청
     * @return 리콜 퀴즈 생성 응답
     */
    @Operation(summary = "날짜 기반 리콜 퀴즈 생성", description = "대상 날짜의 TIL 원본을 기반으로 리콜 퀴즈를 생성")
    @PostMapping("/quizzes")
    public ApiResponse<RecallQuizGenerateResponse> generate(
            Authentication authentication,
            @Valid @RequestBody RecallQuizGenerateRequest request) {

        UUID userId = currentUserId(authentication);
        RecallQuizGenerateResponse response = recallQuizGenerationService.generate(userId, request);

        return ApiResponse.success(response);
    }

    /** 인증 정보에서 사용자 ID 추출 */
    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
