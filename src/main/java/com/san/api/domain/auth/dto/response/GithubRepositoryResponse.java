package com.san.api.domain.auth.dto.response;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.global.external.github.dto.GithubRepository;

public record GithubRepositoryResponse(
        Long githubRepositoryId,
        String name,
        String fullName,
        boolean privateRepository,
        String defaultBranch,
        String htmlUrl
) {
    public static GithubRepositoryResponse from(GithubRepository repository) {
        return new GithubRepositoryResponse(
                repository.id(),
                repository.name(),
                repository.fullName(),
                repository.privateRepository(),
                repository.defaultBranch(),
                repository.htmlUrl()
        );
    }

    public static GithubRepositoryResponse from(GithubRepositoryConnection connection) {
        return new GithubRepositoryResponse(
                connection.getGithubRepositoryId(),
                connection.getName(),
                connection.getFullName(),
                connection.isPrivateRepository(),
                connection.getDefaultBranch(),
                connection.getHtmlUrl()
        );
    }
}
