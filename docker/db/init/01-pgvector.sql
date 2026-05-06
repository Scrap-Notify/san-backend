CREATE EXTENSION IF NOT EXISTS vector;

CREATE UNIQUE INDEX IF NOT EXISTS uk_async_jobs_active_target_job_type
    ON async_jobs (target_id, job_type)
    WHERE status IN ('PENDING', 'PROCESSING');
