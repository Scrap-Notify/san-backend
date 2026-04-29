package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GithubLoginRequest(
        @NotBlank(message = "GitHub 인증 코드를 입력해주세요.")
        String code
) {
}
