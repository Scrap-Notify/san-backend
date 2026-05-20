package com.san.api.global.external.ai.dto.response;

import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;

import java.util.List;

/** GitHub Star recommendation response from the AI server. */
public record AiGithubStarRecommendationResponse(
        List<AiAnalyzeRequest> recommendations
) {
}
