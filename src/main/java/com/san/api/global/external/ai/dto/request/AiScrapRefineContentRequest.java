package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 수집 원본 정제 콘텐츠 요청 DTO */
public record AiScrapRefineContentRequest(
        @JsonProperty("input_type")
        String inputType,
        String content
) {
}
