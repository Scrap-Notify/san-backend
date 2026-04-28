package com.san.api.global.async.repository;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.enums.JobStatusEnum;
import com.san.api.global.async.enums.JobTypeEnum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    List<AsyncJob> findByTargetIdAndJobType(UUID targetId, JobTypeEnum jobType);

    List<AsyncJob> findByStatus(JobStatusEnum status);
}
