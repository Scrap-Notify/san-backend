package com.san.api.global.external.github.dto;

/** GitHub API 사용자 프로필 응답 DTO. */
public record GithubUserProfile(
        Long id,
        String login
) {
}
