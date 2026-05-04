package com.san.api.global.async.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 비동기 잡 생성 및 이벤트 발행을 캡슐화하는 서비스.
 *
 * 도메인 서비스에서 enqueue() 한 줄로 비동기 작업을 요청할 수 있도록 한다.
 */
@Service
@RequiredArgsConstructor
public class AsyncJobManager {

    private static final List<JobStatus> ACTIVE_STATUSES =
            List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final AsyncJobRepository asyncJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UUID enqueue(JobType jobType, UUID targetId) {
        if (asyncJobRepository.existsByTargetIdAndJobTypeAndStatusIn(targetId, jobType, ACTIVE_STATUSES)) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE, "이미 동일한 작업이 진행 중입니다.");
        }

        AsyncJob job = asyncJobRepository.save(
                AsyncJob.builder()
                        .jobType(jobType)
                        .targetId(targetId)
                        .build()
        );

        eventPublisher.publishEvent(new JobCreatedEvent(job.getJobId(), jobType, targetId));

        return job.getJobId();
    }

    @Transactional(readOnly = true)
    public AsyncJob getJob(UUID jobId) {
        return findJob(jobId);
    }

    @Transactional
    public void markProcessing(UUID jobId) {
        findJob(jobId).updateStatus(JobStatus.PROCESSING);
    }

    @Transactional
    public void markCompleted(UUID jobId) {
        findJob(jobId).updateStatus(JobStatus.COMPLETED);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        findJob(jobId).fail(errorMessage);
    }

    private AsyncJob findJob(UUID jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "작업을 찾을 수 없습니다."));
    }
}
