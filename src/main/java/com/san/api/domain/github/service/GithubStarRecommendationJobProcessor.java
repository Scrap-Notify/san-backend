package com.san.api.domain.github.service;

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

/** GitHub Star 추천 비동기 작업 Processor */
@Component
@RequiredArgsConstructor
public class GithubStarRecommendationJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final GithubStarRecommendationAnalysisService githubStarRecommendationAnalysisService;

    /** GitHub Star 추천 작업 생성 이벤트 수신 */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.GITHUB_STAR_RECOMMENDATION) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * GitHub Star 추천 URL 분석 작업 처리
     *
     * targetId는 추천을 요청한 사용자 ID로 사용한다.
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        try {
            githubStarRecommendationAnalysisService.analyzeAndSave(targetId, jobId);
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            asyncJobManager.markFailed(jobId, resolveErrorMessage(e, "GitHub Star 추천 URL 분석 작업 처리 중 오류가 발생했습니다."));
        }
    }
}
