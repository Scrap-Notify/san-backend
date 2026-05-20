package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiScrapRefineResponseTest {

    @Test
    void deserialize_mapsCardMarkdownToRefinedContent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = """
                {
                  "title": "Refined title",
                  "card_markdown": "## Refined content",
                  "embedding": [0.1, 0.2]
                }
                """;

        AiScrapRefineResponse response = objectMapper.readValue(responseBody, AiScrapRefineResponse.class);

        assertThat(response.title()).isEqualTo("Refined title");
        assertThat(response.cardMarkdown()).isEqualTo("## Refined content");
        assertThat(response.refinedContent()).isEqualTo("## Refined content");
        assertThat(response.embedding()).containsExactly(0.1f, 0.2f);
    }
}
