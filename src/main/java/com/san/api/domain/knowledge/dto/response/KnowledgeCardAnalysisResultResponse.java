package com.san.api.domain.knowledge.dto.response;

import com.san.api.global.async.enums.JobStatusEnum;

import java.util.List;
import java.util.UUID;

/** 지식카드 분석 작업 결과 응답 DTO */
public record KnowledgeCardAnalysisResultResponse(
        UUID jobId,
        JobStatusEnum status,
        KnowledgeCardResponse card,
        List<KnowledgeCardResponse> relatedCards,
        String errorMessage
) {
}
