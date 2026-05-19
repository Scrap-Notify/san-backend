package com.san.api.domain.knowledge.service;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** 지식카드 AI 분석 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class KnowledgeCardAnalysisJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final KnowledgeCardAnalysisService knowledgeCardAnalysisService;

    /**
     * CARD_ANALYSIS 작업 생성 이벤트 수신
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.CARD_ANALYSIS) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * 지식카드 AI 분석 작업 처리
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 분석 대상 Scrap ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        try {
            knowledgeCardAnalysisService.createKnowledgeCard(targetId);
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            asyncJobManager.markFailed(jobId, resolveErrorMessage(e, "지식카드 AI 분석 작업 처리 중 오류가 발생했습니다."));
        }
    }
}
