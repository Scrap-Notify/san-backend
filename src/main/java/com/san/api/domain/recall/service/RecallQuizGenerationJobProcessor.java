package com.san.api.domain.recall.service;

import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** 리콜 퀴즈 생성 비동기 작업 Processor */
@Component
@RequiredArgsConstructor
public class RecallQuizGenerationJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final RecallQuizGenerationRepository recallQuizGenerationRepository;
    private final RecallQuizGenerationService recallQuizGenerationService;

    /**
     * RECALL_QUIZ_GENERATION 작업 생성 이벤트 처리
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.RECALL_QUIZ_GENERATION) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * 리콜 퀴즈 생성 작업 처리
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 리콜 퀴즈 생성 ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        try {
            RecallQuizGeneration generation = recallQuizGenerationRepository.findByGenerationIdWithUser(targetId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            RecallQuizGenerateRequest request = new RecallQuizGenerateRequest(
                    generation.getTargetDate(),
                    generation.getQuizType()
            );

            recallQuizGenerationService.generate(generation.getUser().getUserId(), request);
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            asyncJobManager.markFailed(jobId, resolveErrorMessage(e, "리콜 퀴즈 생성 작업 처리 중 오류가 발생했습니다."));
        }
    }
}
