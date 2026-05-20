package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiScrapRefineRequestTest {

    @Test
    void serialize_createsCardRefineRequestBody() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiScrapRefineRequest request = new AiScrapRefineRequest("url", "https://example.com");

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(jsonNode.get("content").get("input_type").asText()).isEqualTo("url");
        assertThat(jsonNode.get("content").get("content").asText()).isEqualTo("https://example.com");
        assertThat(request.cardContent().inputType()).isEqualTo("url");
        assertThat(request.cardContent().content()).isEqualTo("https://example.com");
    }
}
