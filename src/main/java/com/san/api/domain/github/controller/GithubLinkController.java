package com.san.api.domain.github.controller;

import com.san.api.domain.github.service.GithubLinkService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

/** 로그인된 서비스 계정의 GitHub 계정 연동 요청을 처리하는 컨트롤러. */
@Tag(name = "GitHub Link", description = "GitHub 계정 연동 API")
@RestController
@RequestMapping("/api/github/link")
@RequiredArgsConstructor
public class GithubLinkController {

    private final GithubLinkService githubLinkService;

    @Operation(
            summary = "GitHub 계정 연동 시작",
            description = "현재 로그인한 서비스 계정에 GitHub 계정을 연결하기 위해 GitHub authorize 페이지로 리다이렉트합니다."
    )
    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView authorize(Authentication authentication) {
        return new RedirectView(githubLinkService.createLinkAuthorizationRedirectUrl(currentUserId(authentication)));
    }

    @Operation(
            summary = "GitHub 계정 연동 해제",
            description = "현재 로그인한 서비스 계정의 GitHub 계정과 연결된 레포지토리 정보를 삭제합니다."
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> unlink(Authentication authentication) {
        githubLinkService.unlinkGithubAccount(currentUserId(authentication));
        return ApiResponse.success();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
