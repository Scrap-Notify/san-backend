package com.san.api.global.async.entity;

import com.san.api.global.async.enums.JobStatusEnum;
import com.san.api.global.async.enums.JobTypeEnum;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "async_jobs")
public class AsyncJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id", columnDefinition = "uuid", updatable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobTypeEnum jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatusEnum status;

    @Column(name = "target_id", nullable = false, columnDefinition = "uuid")
    private UUID targetId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Builder
    public AsyncJob(JobTypeEnum jobType, UUID targetId) {
        this.jobType = jobType;
        this.targetId = targetId;
        this.status = JobStatusEnum.PENDING;
    }

    /**
     * 잡 상태를 변경합니다.
     *
     * @param status 변경할 상태 (PROCESSING, COMPLETED 등)
     */
    public void updateStatus(JobStatusEnum status) {
        this.status = status;
    }

    /**
     * 잡을 실패 상태로 전환하고 에러 메시지를 기록합니다.
     *
     * @param errorMessage 실패 원인 메시지
     */
    public void fail(String errorMessage) {
        this.status = JobStatusEnum.FAILED;
        this.errorMessage = errorMessage;
    }
}
