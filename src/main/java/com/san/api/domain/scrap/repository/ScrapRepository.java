package com.san.api.domain.scrap.repository;

import com.san.api.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 스크랩 엔티티 Repository */
public interface ScrapRepository extends JpaRepository<Scrap, UUID> {

    /**
     * 특정 날짜에 수집된 스크랩의 지식 카드 ID 목록 조회.
     * TIL 리콜 추천 시 원본 카드를 제외하기 위해 사용.
     */
    @Query(value = """
            SELECT kc.card_id
            FROM scraps s
            JOIN knowledge_cards kc ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND CAST(s.created_at AS date) = :targetDate
              AND s.is_deleted = false
              AND kc.is_deleted = false
            """, nativeQuery = true)
    List<UUID> findCardIdsByUserAndDate(
            @Param("userId") UUID userId,
            @Param("targetDate") LocalDate targetDate
    );
}
