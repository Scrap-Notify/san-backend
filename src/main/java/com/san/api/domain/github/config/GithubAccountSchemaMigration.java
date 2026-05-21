package com.san.api.domain.github.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Removes the legacy global unique constraint on github_accounts.github_user_id.
 * Multiple service users may link the same GitHub account, while user_id remains unique.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GithubAccountSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT con.conname
                FROM pg_constraint con
                JOIN pg_class rel ON rel.oid = con.conrelid
                WHERE con.contype = 'u'
                  AND rel.oid = 'github_accounts'::regclass
                  AND con.conkey = ARRAY[
                      (
                          SELECT att.attnum
                          FROM pg_attribute att
                          WHERE att.attrelid = rel.oid
                            AND att.attname = 'github_user_id'
                            AND NOT att.attisdropped
                      )
                  ]::smallint[]
                """, String.class);

        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("ALTER TABLE github_accounts DROP CONSTRAINT " + quoteIdentifier(constraintName));
            log.info("[GitHub] dropped legacy unique constraint on github_user_id: {}", constraintName);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
