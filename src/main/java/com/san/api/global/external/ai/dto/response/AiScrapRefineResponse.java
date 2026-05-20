package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 수집 원본 정제 응답 DTO */
public record AiScrapRefineResponse(
        String title,

        @JsonProperty("card_markdown")
        String cardMarkdown,

        float[] embedding
) {

    public String refinedContent() {
        return cardMarkdown;
    }
}
