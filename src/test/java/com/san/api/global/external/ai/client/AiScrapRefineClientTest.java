package com.san.api.global.external.ai.client;

import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiScrapRefineClientTest {

    @Test
    void refine_callsCardApiAndReturnsCardMarkdown() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiScrapRefineClient client = new AiScrapRefineClient(builder.build());

        server.expect(requestTo("http://ai.test/ai/card"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "content": {
                            "input_type": "text",
                            "content": "raw content"
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "title": "Refined title",
                          "card_markdown": "## Refined content",
                          "embedding": [0.1, 0.2]
                        }
                        """, MediaType.APPLICATION_JSON));

        AiScrapRefineResponse response = client.refine(new AiScrapRefineRequest("text", "raw content"));

        assertThat(response.title()).isEqualTo("Refined title");
        assertThat(response.refinedContent()).isEqualTo("## Refined content");
        assertThat(response.embedding()).containsExactly(0.1f, 0.2f);
        server.verify();
    }
}
