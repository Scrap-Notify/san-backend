package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Error response body returned by the AI server. */
public record AiErrorResponse(
        @JsonProperty("error_code")
        @JsonAlias("code")
        String errorCode,
        String message
) {
}
