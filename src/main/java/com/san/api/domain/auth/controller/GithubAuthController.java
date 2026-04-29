package com.san.api.domain.auth.controller;

import com.san.api.domain.auth.dto.request.GithubLoginRequest;
import com.san.api.domain.auth.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.auth.dto.request.GithubTokenExchangeRequest;
import com.san.api.domain.auth.dto.response.GithubRepositoryResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.service.GithubAuthService;
import com.san.api.domain.auth.service.GithubRepositoryService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.UUID;

@Tag(name = "GitHub Auth", description = "GitHub OAuth API")
@RestController
@RequestMapping("/api/auth/github")
@RequiredArgsConstructor
public class GithubAuthController {

    private final GithubAuthService githubAuthService;
    private final GithubRepositoryService githubRepositoryService;

    /**
     * GitHub OAuth authorize 페이지로 리다이렉트합니다.
     *
     * 프론트엔드는 GitHub 로그인 버튼 클릭 시 이 엔드포인트로 이동시키면 됩니다.
     * 백엔드는 CSRF 방어용 state를 발급하고 GitHub authorize URL을 생성합니다.
     */
    @Operation(summary = "GitHub OAuth 시작")
    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView authorize() {
        return new RedirectView(githubAuthService.createAuthorizationRedirectUrl());
    }

    /**
     * GitHub OAuth callback을 처리하고 프론트엔드 성공 페이지로 리다이렉트합니다.
     *
     * 토큰을 URL에 직접 노출하지 않고, 짧은 TTL의 일회용 ticket만 전달합니다.
     * 프론트엔드는 전달받은 ticket으로 토큰 교환 API를 호출합니다.
     */
    @Operation(summary = "GitHub OAuth 콜백")
    @GetMapping("/callback")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView callback(
            @RequestParam String code,
            @RequestParam String state) {
        return new RedirectView(githubAuthService.handleCallback(code, state));
    }

    /**
     * GitHub OAuth callback에서 발급된 일회용 ticket을 서비스 JWT 토큰 쌍으로 교환합니다.
     */
    @Operation(summary = "GitHub 로그인 티켓 교환")
    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> exchangeToken(@Valid @RequestBody GithubTokenExchangeRequest request) {
        return ApiResponse.success(githubAuthService.exchangeToken(request));
    }

    /**
     * GitHub OAuth authorization code로 로그인합니다.
     *
     * 프론트엔드가 직접 code를 전달하는 방식도 유지합니다. 로컬 테스트나 Postman 검증에 유용합니다.
     */
    @Operation(summary = "GitHub 로그인")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody GithubLoginRequest request) {
        return ApiResponse.success(githubAuthService.login(request));
    }

    /**
     * 로그인 사용자가 접근 가능한 GitHub 저장소 목록을 조회합니다.
     */
    @Operation(summary = "GitHub 레포 목록 조회")
    @GetMapping("/repositories")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<GithubRepositoryResponse>> repositories(Authentication authentication) {
        return ApiResponse.success(githubRepositoryService.findRepositories(currentUserId(authentication)));
    }

    /**
     * 사용자가 선택한 GitHub 저장소를 서비스 계정에 연결합니다.
     */
    @Operation(summary = "GitHub 레포 연결")
    @PostMapping("/repositories/connect")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GithubRepositoryResponse> connectRepository(
            Authentication authentication,
            @Valid @RequestBody GithubRepositoryConnectRequest request) {
        return ApiResponse.success(githubRepositoryService.connectRepository(currentUserId(authentication), request));
    }

    /**
     * 서비스에 연결된 GitHub 저장소 목록을 조회합니다.
     */
    @Operation(summary = "연결된 GitHub 레포 조회")
    @GetMapping("/repositories/connected")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<GithubRepositoryResponse>> connectedRepositories(Authentication authentication) {
        return ApiResponse.success(githubRepositoryService.findConnectedRepositories(currentUserId(authentication)));
    }

    /**
     * 서비스에 연결된 GitHub 저장소를 해제합니다.
     */
    @Operation(summary = "GitHub 레포 연결 해제")
    @DeleteMapping("/repositories/{repositoryId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> disconnectRepository(
            Authentication authentication,
            @PathVariable Long repositoryId) {
        githubRepositoryService.disconnectRepository(currentUserId(authentication), repositoryId);
        return ApiResponse.success();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
