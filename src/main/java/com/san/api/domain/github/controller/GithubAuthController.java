package com.san.api.domain.github.controller;

import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.github.dto.request.GithubLoginRequest;
import com.san.api.domain.github.dto.request.GithubTokenExchangeRequest;
import com.san.api.domain.github.service.GithubAuthService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/** GitHub OAuth 로그인 요청을 처리하는 컨트롤러. */
@Tag(name = "GitHub Auth", description = "GitHub OAuth 로그인 API")
@RestController
@RequestMapping("/api/auth/github")
@RequiredArgsConstructor
public class GithubAuthController {

    private final GithubAuthService githubAuthService;

    @Operation(
            summary = "GitHub OAuth 시작",
            description = "state를 발급하고 GitHub authorize 페이지로 리다이렉트합니다."
    )
    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView authorize() {
        return new RedirectView(githubAuthService.createAuthorizationRedirectUrl());
    }

    @Operation(
            summary = "GitHub OAuth callback",
            description = "GitHub가 전달한 code와 state를 검증하고 일회용 로그인 ticket을 발급합니다."
    )
    @GetMapping("/callback")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView callback(
            @RequestParam String code,
            @RequestParam String state) {
        return new RedirectView(githubAuthService.handleCallback(code, state));
    }

    @Operation(
            summary = "GitHub 로그인 ticket 교환",
            description = "OAuth callback 이후 프론트엔드가 받은 일회용 ticket을 access/refresh token으로 교환합니다."
    )
    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> exchangeToken(@Valid @RequestBody GithubTokenExchangeRequest request) {
        return ApiResponse.success(githubAuthService.exchangeToken(request));
    }

    @Operation(
            summary = "GitHub 로그인",
            description = "GitHub authorization code를 받아 서비스 JWT 토큰 쌍을 발급합니다."
    )
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody GithubLoginRequest request) {
        return ApiResponse.success(githubAuthService.login(request));
    }
}
