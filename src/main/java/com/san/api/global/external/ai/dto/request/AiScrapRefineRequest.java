package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 수집 원본 정제 요청 DTO */
public record AiScrapRefineRequest(
        @JsonProperty("content")
        AiScrapRefineContentRequest cardContent
) {

    public AiScrapRefineRequest(String inputType, String content) {
        this(new AiScrapRefineContentRequest(inputType, content));
    }
}
