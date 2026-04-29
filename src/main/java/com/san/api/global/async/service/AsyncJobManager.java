package com.san.api.global.async.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.enums.JobTypeEnum;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.repository.AsyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비동기 잡 생성 및 이벤트 발행을 캡슐화하는 서비스.
 *
 * 도메인 서비스에서 enqueue() 한 줄로 비동기 작업을 요청할 수 있도록 한다.
 */
@Service
@RequiredArgsConstructor
public class AsyncJobManager {

    private final AsyncJobRepository asyncJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UUID enqueue(JobTypeEnum jobType, UUID targetId) {
        AsyncJob job = asyncJobRepository.save(
                AsyncJob.builder()
                        .jobType(jobType)
                        .targetId(targetId)
                        .build()
        );

        eventPublisher.publishEvent(new JobCreatedEvent(job.getJobId(), jobType, targetId));

        return job.getJobId();
    }
}
