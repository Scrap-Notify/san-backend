package com.san.api.domain.til.controller;

import com.san.api.domain.til.dto.response.TilGithubCommitJobResponse;
import com.san.api.domain.til.service.TilGithubCommitService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** TIL GitHub 커밋 API Controller */
@Tag(name = "TIL GitHub Commit", description = "TIL GitHub 커밋 API")
@RestController
@RequestMapping("/til")
@RequiredArgsConstructor
public class TilGithubCommitController {

    private final TilGithubCommitService tilGithubCommitService;

    /**
     * TIL GitHub 커밋 작업을 등록합니다.
     *
     * @param authentication 인증 정보
     * @param summaryId 커밋할 TIL ID
     * @return 등록된 TIL GitHub 커밋 작업 정보
     */
    @Operation(summary = "TIL GitHub 커밋 작업 등록", description = "생성된 TIL을 연결된 GitHub 저장소에 커밋하는 비동기 작업을 등록")
    @PostMapping("/{summaryId}/github-commit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<TilGithubCommitJobResponse> commitTilToGithub(
            Authentication authentication,
            @PathVariable UUID summaryId) {

        TilGithubCommitService.RequestResult result = tilGithubCommitService.requestCommit(
                currentUserId(authentication),
                summaryId
        );

        return ApiResponse.success(TilGithubCommitJobResponse.from(result));
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
