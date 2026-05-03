package com.san.api.domain.knowledge.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.enums.JobTypeEnum;
import com.san.api.global.async.enums.JobStatusEnum;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.executor.AsyncJobProcessor;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 지식카드 AI 분석 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class KnowledgeCardAnalysisJobProcessor implements AsyncJobProcessor {

    private final AsyncJobRepository asyncJobRepository;

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
    @Transactional
    public void process(UUID jobId, UUID targetId) {
        AsyncJob job = getJob(jobId);

        try {
            job.updateStatus(JobStatusEnum.PROCESSING);
            job.updateStatus(JobStatusEnum.COMPLETED);
        } catch (Exception e) {
            job.fail(resolveErrorMessage(e));
        }
    }

    /**
     * 비동기 작업 조회
     *
     * @param jobId 비동기 작업 ID
     * @return 비동기 작업
     */
    private AsyncJob getJob(UUID jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "작업을 찾을 수 없습니다."));
    }

    /**
     * 실패 메시지 정리
     *
     * @param exception 발생 예외
     * @return 저장할 실패 메시지
     */
    private String resolveErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "지식카드 AI 분석 작업 처리 중 오류가 발생했습니다.";
        }

        return message;
    }
}
