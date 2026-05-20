package com.san.api.domain.github.controller;

import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.service.GithubStarRecommendationJobService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** GitHub Star 추천 API Controller */
@Tag(name = "GitHub Star Recommendation", description = "GitHub Star 추천 API")
@RestController
@RequestMapping("/github/star-recommendations")
@RequiredArgsConstructor
public class GithubStarRecommendationController {

    private final GithubStarRecommendationJobService githubStarRecommendationJobService;

    /**
     * GitHub Star 추천 작업 요청
     *
     * @param authentication 인증 사용자 정보
     * @return GitHub Star 추천 작업 응답
     */
    @Operation(
            summary = "GitHub Star 추천 작업 요청",
            description = "최초 요청이면 GitHub Star 추천 작업을 등록하고, 이미 추천 후보가 있으면 기존 추천 후보를 반환"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<GithubStarRecommendationJobResponse> requestRecommendation(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        GithubStarRecommendationJobResponse response = githubStarRecommendationJobService.requestRecommendation(userId);

        return ApiResponse.success(response);
    }

    /**
     * 인증 정보의 사용자 ID 추출
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
