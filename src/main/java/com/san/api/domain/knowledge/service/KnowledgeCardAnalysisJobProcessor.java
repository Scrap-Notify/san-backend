package com.san.api.domain.knowledge.service;

import com.san.api.global.async.enums.JobTypeEnum;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.executor.AsyncJobProcessor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 지식카드 AI 분석 비동기 작업 처리기 */
@Component
public class KnowledgeCardAnalysisJobProcessor implements AsyncJobProcessor {

    /**
     * CARD_ANALYSIS 작업 생성 이벤트 수신
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("asyncJobExecutor")
    @EventListener
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobTypeEnum.CARD_ANALYSIS) {
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
    }
}
