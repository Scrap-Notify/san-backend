package com.san.api.domain.feedback.dto.response;

import java.util.UUID;

/** 서비스 피드백 등록 응답 DTO. */
public record FeedbackCreateResponse(
        /** 등록된 피드백 ID. */
        UUID feedbackId
) {
    /** 등록된 피드백 ID로 응답 DTO를 생성합니다. */
    public static FeedbackCreateResponse of(UUID feedbackId) {
        return new FeedbackCreateResponse(feedbackId);
    }
}
