package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GithubTokenExchangeRequest(
        @NotBlank(message = "GitHub 로그인 티켓을 입력해주세요.")
        String ticket
) {
}
