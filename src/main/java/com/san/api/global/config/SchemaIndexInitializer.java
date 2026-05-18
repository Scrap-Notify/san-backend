package com.san.api.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_async_jobs_active_target_job_type
                    ON async_jobs (target_id, job_type)
                    WHERE status IN ('PENDING', 'PROCESSING')
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_scraps_active_user_source_hash
                    ON scraps (user_id, source_type, content_hash)
                    WHERE is_deleted = false
                      AND content_hash IS NOT NULL
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_event_domain_time
                    ON audit_log_events (event_domain, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_outcome_time
                    ON audit_log_events (outcome, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_failure_reason_time
                    ON audit_log_events (failure_reason_code, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_target_time
                    ON audit_log_events (target_type, target_id, occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_audit_log_events_occurred_at
                    ON audit_log_events (occurred_at DESC)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE audit_log_events
                    ADD COLUMN IF NOT EXISTS integrity_hash varchar(64)
                """);
    }
}
