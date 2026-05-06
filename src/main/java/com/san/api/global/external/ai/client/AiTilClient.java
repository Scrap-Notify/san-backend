package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiTilRequest;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** AI TIL 생성 Client */
@Slf4j
@Component
public class AiTilClient {

    private final RestClient restClient;

    /** 공통 설정이 적용된 AI 서버 호출 Client를 주입받는다. */
    public AiTilClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * AI 서버에 TIL 생성 요청
     *
     * @param request AI TIL 생성 요청
     * @return AI TIL 생성 응답
     */
    public AiTilResponse generateTil(AiTilRequest request) {
        try {
            AiTilResponse response = restClient.post()
                    .uri("/ai/til")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiTilResponse.class);

            validateResponse(response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("AI TIL generation request failed: {}", e.getMessage(), e);
            throw new BusinessException(AiErrorCode.AI_TIL_GENERATION_FAILED);
        }
    }

    /**
     * AI TIL 생성 응답 필수값 검증
     *
     * @param response AI TIL 생성 응답
     */
    private void validateResponse(AiTilResponse response) {
        if (response == null
                || isBlank(response.tilMarkdown())
                || response.embedding() == null
                || response.embedding().length == 0) {
            throw new BusinessException(AiErrorCode.AI_TIL_INVALID_RESPONSE);
        }
    }

    /** 빈 문자열 여부 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
