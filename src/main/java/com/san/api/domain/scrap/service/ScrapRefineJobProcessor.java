package com.san.api.domain.scrap.service;

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

/** 수집 원본 정제 비동기 작업 Processor */
@Component
@RequiredArgsConstructor
public class ScrapRefineJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final ScrapRefineService scrapRefineService;

    /**
     * SCRAP_REFINE 작업 생성 이벤트 처리
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("githubJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.SCRAP_REFINE) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * 수집 원본 정제 작업 처리
     *
     * @param jobId    비동기 작업 ID
     * @param targetId 정제 대상 Scrap ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        try {
            scrapRefineService.refine(targetId);
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            asyncJobManager.markFailed(jobId, resolveErrorMessage(e, "수집 원본 정제 작업 처리 중 오류가 발생했습니다."));
        }
    }
}
