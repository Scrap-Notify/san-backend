package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** AI 서버 지식카드 분석 Client */
@Component
public class AiAnalysisClient {

    private final RestClient restClient;

    public AiAnalysisClient(@Value("${ai.server.base-url}") String aiServerUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .build();
    }

    /**
     * AI 서버에 지식카드 분석 요청
     *
     * @param request AI 분석 요청
     * @return AI 분석 응답
     */
    public AiAnalyzeResponse analyze(AiAnalyzeRequest request) {
        try {
            AiAnalyzeResponse response = restClient.post()
                    .uri("/ai/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiAnalyzeResponse.class);

            validateResponse(response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(AiErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    /**
     * 지식카드 생성에 필요한 최소 응답값 검증
     *
     * @param response AI 분석 응답
     */
    private void validateResponse(AiAnalyzeResponse response) {
        if (response == null
                || isBlank(response.title())
                || isBlank(response.summary())
                || isBlank(response.category())
                || response.embedding() == null
                || response.embedding().length == 0) {
            throw new BusinessException(AiErrorCode.AI_ANALYSIS_INVALID_RESPONSE);
        }
    }

    /**
     * 빈 문자열 여부
     *
     * @param value 검증 대상 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
