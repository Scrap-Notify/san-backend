package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** TIL 생성 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class TilGenerationJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final DailySummaryService dailySummaryService;
    private final TilGenerationService tilGenerationService;

    /**
     * TIL_GENERATION 작업 생성 이벤트 수신
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("asyncJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.TIL_GENERATION) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * TIL 생성 작업 처리
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 생성 대상 DailySummary ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        try {
            DailySummary summary = dailySummaryService.getSummary(targetId);
            AiTilResponse response = tilGenerationService.generate(
                    summary.getUser().getUserId(),
                    summary.getTargetDate()
            );

            dailySummaryService.updateGeneratedResult(
                    summary.getSummaryId(),
                    response.title(),
                    response.tilMarkdown(),
                    response.embedding()
            );
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            asyncJobManager.markFailed(jobId, resolveErrorMessage(e, "TIL 생성 작업 처리 중 오류가 발생했습니다."));
        }
    }
}
