package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiScrapRefineRequestTest {

    @Test
    void toTilRequest_convertsToSingleContentTilGenerationRequest() {
        AiScrapRefineRequest request = new AiScrapRefineRequest("TEXT", "raw content");

        AiTilRequest tilRequest = request.toTilRequest();

        assertThat(tilRequest.generateTil()).isTrue();
        assertThat(tilRequest.contents()).hasSize(1);
        assertThat(tilRequest.contents().get(0).inputType()).isEqualTo("TEXT");
        assertThat(tilRequest.contents().get(0).content()).isEqualTo("raw content");
    }

    @Test
    void serialize_createsCardRefineRequestBody() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiScrapRefineRequest request = new AiScrapRefineRequest("url", "https://example.com");

        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(jsonNode.get("content").get("input_type").asText()).isEqualTo("url");
        assertThat(jsonNode.get("content").get("content").asText()).isEqualTo("https://example.com");
        assertThat(request.inputType()).isEqualTo("url");
        assertThat(request.content()).isEqualTo("https://example.com");
    }
}
