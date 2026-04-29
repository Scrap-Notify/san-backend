package com.san.api.global.async.dto.response;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.enums.JobStatusEnum;
import com.san.api.global.async.enums.JobTypeEnum;

import java.util.UUID;

public record AsyncJobStatusResponse(
        UUID jobId,
        JobTypeEnum jobType,
        JobStatusEnum status,
        String errorMessage
) {
    public static AsyncJobStatusResponse from(AsyncJob job) {
        return new AsyncJobStatusResponse(
                job.getJobId(),
                job.getJobType(),
                job.getStatus(),
                job.getErrorMessage()
        );
    }
}
