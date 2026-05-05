package com.san.api.domain.til.controller;

import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.service.TilService;
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

/** TIL API Controller */
@Tag(name = "TIL", description = "TIL API")
@RestController
@RequestMapping("/api/til")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    /**
     * TIL 생성 작업 등록
     *
     * @param authentication 인증 정보
     * @param request TIL 생성 작업 등록 요청
     * @return 등록된 TIL 생성 작업 응답
     */
    @Operation(summary = "TIL 생성 작업 등록", description = "대상 날짜의 지식카드 원본을 기반으로 TIL을 생성하는 비동기 작업을 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<TilGenerationJobResponse> generateTil(
            Authentication authentication,
            @Valid @RequestBody TilGenerateRequest request) {

        UUID userId = currentUserId(authentication);
        TilGenerationJobResponse response = tilService.requestGeneration(userId, request);

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
