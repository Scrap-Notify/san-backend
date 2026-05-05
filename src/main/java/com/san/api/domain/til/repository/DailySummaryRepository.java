package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** 매일의 요약 Repository */
public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

    /**
     * 사용자와 대상 날짜 기준 매일의 요약 조회
     *
     * @param userId 사용자 ID
     * @param targetDate 요약 대상 날짜
     * @return 조회된 매일의 요약
     */
    Optional<DailySummary> findByUser_UserIdAndTargetDate(UUID userId, LocalDate targetDate);

    /**
     * 사용자 정보를 함께 조회하는 매일의 요약 단건 조회
     *
     * @param summaryId 매일의 요약 ID
     * @return 사용자 정보가 포함된 매일의 요약
     */
    @Query("""
            SELECT ds
            FROM DailySummary ds
            JOIN FETCH ds.user
            WHERE ds.summaryId = :summaryId
            """)
    Optional<DailySummary> findBySummaryIdWithUser(@Param("summaryId") UUID summaryId);
}
