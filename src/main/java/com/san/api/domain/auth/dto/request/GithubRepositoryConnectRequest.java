package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record GithubRepositoryConnectRequest(
        @NotNull(message = "GitHub 저장소 ID를 입력해주세요.")
        Long githubRepositoryId
) {
}
