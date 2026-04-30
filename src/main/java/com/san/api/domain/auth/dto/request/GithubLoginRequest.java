package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** GitHub authorization code 로그인 요청 DTO. */
public record GithubLoginRequest(
        @NotBlank(message = "GitHub 인증 코드를 입력해주세요.")
        String code
) {
}
