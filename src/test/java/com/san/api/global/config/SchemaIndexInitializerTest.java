package com.san.api.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchemaIndexInitializerTest {

    @Test
    void createsAuditLogSearchIndexes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SchemaIndexInitializer initializer = new SchemaIndexInitializer(jdbcTemplate);

        initializer.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).execute(contains("idx_audit_log_events_event_domain_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_outcome_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_failure_reason_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_target_time"));
        verify(jdbcTemplate).execute(contains("idx_audit_log_events_occurred_at"));
    }
}
