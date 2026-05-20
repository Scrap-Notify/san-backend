package com.san.api.domain.github.dto.response;

import com.san.api.domain.github.entity.GithubStarRecommendation;

import java.util.UUID;

/** GitHub Star 추천 후보 응답 DTO */
public record GithubStarRecommendationResponse(
        UUID recommendationId,
        String url,
        String title,
        String summary,
        boolean collected
) {

    public static GithubStarRecommendationResponse from(GithubStarRecommendation recommendation) {
        return new GithubStarRecommendationResponse(
                recommendation.getGithubStarRecommendationId(),
                recommendation.getUrl(),
                recommendation.getTitle(),
                recommendation.getSummary(),
                recommendation.isCollected()
        );
    }
}
