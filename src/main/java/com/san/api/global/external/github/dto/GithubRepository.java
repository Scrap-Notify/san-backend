package com.san.api.global.external.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepository(
        Long id,
        String name,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("private")
        boolean privateRepository,

        @JsonProperty("default_branch")
        String defaultBranch,

        @JsonProperty("html_url")
        String htmlUrl
) {
}
