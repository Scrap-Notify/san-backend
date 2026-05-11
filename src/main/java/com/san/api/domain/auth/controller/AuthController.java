package com.san.api.domain.auth.controller;

import com.san.api.domain.auth.dto.request.LoginRequest;
import com.san.api.domain.auth.dto.request.ReissueRequest;
import com.san.api.domain.auth.dto.request.SignupRequest;
import com.san.api.domain.auth.dto.request.WithdrawRequest;
import com.san.api.domain.auth.dto.response.AuthSessionListResponse;
import com.san.api.domain.auth.dto.response.SignupResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.auth.service.AuthService;
import com.san.api.domain.auth.service.AuthSessionService;
import com.san.api.global.response.ApiResponse;
import com.san.api.global.security.token.BearerTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    @Operation(summary = "아이디 중복 확인")
    @GetMapping("/check-username")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> checkUsername(
            @RequestParam @NotBlank(message = "아이디를 입력해주세요.") @Size(min = 4, max = 20) @Pattern(regexp = "^[a-z0-9]+$") String username) {
        authService.checkUsernameDuplicate(username);
        return ApiResponse.success();
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/reissue")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return ApiResponse.success(authService.reissue(request));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String accessToken = BearerTokenResolver.resolve(request);
        authService.logout(accessToken);
        return ApiResponse.success();
    }

    /**
     * 현재 로그인한 사용자의 인증 세션 목록을 조회합니다.
     *
     * @param request Bearer access token을 포함한 HTTP 요청
     * @return 현재 세션 여부와 refresh token 만료 시간을 포함한 세션 목록
     */
    @Operation(summary = "인증 세션 목록 조회")
    @GetMapping("/sessions")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthSessionListResponse> getSessions(HttpServletRequest request) {
        String accessToken = BearerTokenResolver.resolve(request);
        return ApiResponse.success(authSessionService.getSessions(accessToken));
    }

    /**
     * 현재 로그인한 사용자의 특정 인증 세션을 폐기합니다.
     *
     * @param request Bearer access token을 포함한 HTTP 요청
     * @param sessionId 폐기할 인증 세션 식별자
     * @param clientType 폐기할 인증 세션의 클라이언트 유형
     * @return 성공 시 빈 응답
     */
    @Operation(summary = "인증 세션 폐기")
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> revokeSession(
            HttpServletRequest request,
            @PathVariable String sessionId,
            @RequestParam ClientType clientType) {
        String accessToken = BearerTokenResolver.resolve(request);
        authSessionService.revokeSession(accessToken, clientType, sessionId);
        return ApiResponse.success();
    }

    @Operation(summary = "회원탈퇴")
    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> withdraw(Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {
        String userId = (String) authentication.getPrincipal();
        authService.withdraw(userId, request);
        return ApiResponse.success();
    }

}
