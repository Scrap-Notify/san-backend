package com.san.api.domain.feedback.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** 등록된 피드백을 Mattermost Incoming Webhook으로 알립니다. */
@Slf4j
@Component
public class MattermostFeedbackNotifier {

    private final RestClient restClient;
    private final String webhookUrl;
    private final int maxAttempts;

    public MattermostFeedbackNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${feedback.mattermost.webhook-url:}") String webhookUrl,
            @Value("${feedback.mattermost.max-attempts:3}") int maxAttempts) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    /**
     * Mattermost webhook URL이 설정된 경우 피드백 알림을 전송합니다.
     * 알림 실패가 피드백 저장 흐름을 막지 않도록 예외는 경고 로그로만 남깁니다.
     */
    @Async("asyncJobExecutor")
    public void notify(FeedbackNotificationPayload payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .body(Map.of("text", createMessage(payload)))
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientException e) {
                log.warn("Failed to send feedback notification to Mattermost. feedbackId={}, attempt={}/{}",
                        payload.feedbackId(), attempt, maxAttempts, e);
            }
        }
    }

    /** Mattermost 채널에 표시할 피드백 메시지를 생성합니다. */
    private String createMessage(FeedbackNotificationPayload payload) {
        return """
                ### 새 피드백이 도착했습니다
                - feedbackId: %s
                - type: %s
                - userId: %s
                - clientType: %s
                - pageUrl: %s
                - traceId: %s
                - contact: %s

                ```text
                %s
                ```
                """.formatted(
                payload.feedbackId(),
                payload.type(),
                valueOrDash(payload.userId()),
                valueOrDash(payload.clientType()),
                valueOrDash(payload.pageUrl()),
                valueOrDash(payload.traceId()),
                valueOrDash(payload.contact()),
                payload.content()
        );
    }

    /** 알림 메시지에서 비어 있는 값을 '-'로 표시합니다. */
    private String valueOrDash(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "-";
        }
        return value.toString();
    }
}
