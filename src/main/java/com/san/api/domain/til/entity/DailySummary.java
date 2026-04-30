package com.san.api.domain.til.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** 매일의 요약 엔티티 */
@Entity
@Table(name = "daily_summaries")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySummary extends BaseEntity {

    @Id
    @Column(name = "summary_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID summaryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;

    @Builder
    public DailySummary(
            User user,
            LocalDate targetDate,
            String content,
            float[] embedding,
            LocalDateTime pushedAt
    ) {
        this.summaryId = UUID.randomUUID();
        this.user = user;
        this.targetDate = targetDate;
        this.content = content;
        this.embedding = embedding;
        this.pushedAt = pushedAt;
    }
}
