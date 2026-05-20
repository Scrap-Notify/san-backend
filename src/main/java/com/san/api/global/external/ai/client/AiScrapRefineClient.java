package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** AI 수집 원본 정제 클라이언트 */
@Slf4j
@Component
public class AiScrapRefineClient {

    private final RestClient restClient;

    /** AI 서버 요청 공통 설정이 적용된 RestClient를 주입한다. */
    public AiScrapRefineClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * AI 서버에 수집 원본 정제를 요청한다.
     *
     * @param request AI 수집 원본 정제 요청
     * @return AI 수집 원본 정제 응답
     */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class, HttpClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiScrapRefineResponse refine(AiScrapRefineRequest request) {
        AiScrapRefineResponse response = restClient.post()
                .uri("/ai/card")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiScrapRefineResponse.class);

        validateResponse(response);
        return response;
    }

    /**
     * 재시도 이후에도 수집 원본 정제 요청이 실패하면 비즈니스 예외로 변환한다.
     *
     * @param e 요청 실패 예외
     * @param request 수집 원본 정제 요청
     * @return 정상 반환하지 않고 예외를 던진다.
     */
    @Recover
    public AiScrapRefineResponse recoverRefine(Exception e, AiScrapRefineRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI scrap refine failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_SCRAP_REFINE_FAILED);
    }

    /**
     * AI 수집 원본 정제 응답 유효성을 검증한다.
     *
     * @param response AI 수집 원본 정제 응답
     */
    private void validateResponse(AiScrapRefineResponse response) {
        if (response == null || isBlank(response.cardMarkdown())) {
            throw new BusinessException(AiErrorCode.AI_SCRAP_REFINE_INVALID_RESPONSE);
        }
    }

    /** 공백 문자열 여부를 확인한다. */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
