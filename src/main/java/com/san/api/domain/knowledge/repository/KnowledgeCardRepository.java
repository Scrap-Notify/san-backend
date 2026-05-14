package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 지식 카드 Repository */
public interface KnowledgeCardRepository extends JpaRepository<KnowledgeCard, UUID> {

    // 수집 원본 기준 지식카드 생성 여부 확인
    boolean existsByScrap_ScrapId(UUID scrapId);

    // 수집 원본 기준 생성된 지식카드와 카테고리 조회
    @Query("""
            SELECT kc
            FROM KnowledgeCard kc
            JOIN FETCH kc.category
            WHERE kc.scrap.scrapId = :scrapId
            """)
    Optional<KnowledgeCard> findByScrapIdWithCategory(@Param("scrapId") UUID scrapId);

    // 로그인 사용자 기준 지식카드 목록 조회
    @Query("""
            SELECT kc
            FROM KnowledgeCard kc
            JOIN kc.scrap s
            JOIN FETCH kc.category
            WHERE s.user.userId = :userId
            ORDER BY kc.createdAt DESC
            """)
    List<KnowledgeCard> findByScrap_User_UserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /**
     * TIL 생성에 사용할 특정 날짜에 수집된 지식카드 원본 조회
     */
    @Query("""
            SELECT kc
            FROM KnowledgeCard kc
            JOIN FETCH kc.scrap s
            JOIN FETCH kc.category
            WHERE s.user.userId = :userId
              AND s.createdAt >= :startAt
              AND s.createdAt < :endAt
            ORDER BY s.createdAt ASC
            """)
    List<KnowledgeCard> findTilSourceCards(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    /**
     * 벡터 유사도 기반 지식 카드 검색.
     * knowledge_cards에 user_id가 없으므로 scraps JOIN으로 권한 필터링.
     */
    @Query(value = """
            SELECT kc.card_id, kc.scrap_id, kc.category_id, kc.title, kc.summary,
                   kc.embedding, kc.created_at, kc.updated_at, kc.is_deleted
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
            ORDER BY kc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<KnowledgeCard> searchByVector(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 벡터 유사도 기반 지식 카드 검색 (태그·카테고리·날짜 필터 + 유사도 threshold 포함).
     * tag, categoryId, fromDate, toDate는 null 전달 시 필터 미적용.
     * threshold는 pgvector 코사인 거리 상한값 (거리 < threshold인 카드만 반환).
     */
    @Query(value = """
            SELECT kc.card_id, kc.scrap_id, kc.category_id, kc.title, kc.summary,
                   kc.embedding, kc.created_at, kc.updated_at, kc.is_deleted
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM card_tags ct JOIN tags t ON ct.tag_id = t.tag_id
                  WHERE ct.card_id = kc.card_id AND t.tag_name = :tag
              ))
              AND (:categoryId IS NULL OR kc.category_id = CAST(:categoryId AS uuid))
              AND (:fromDate IS NULL OR CAST(kc.created_at AS date) >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR CAST(kc.created_at AS date) <= CAST(:toDate AS date))
              AND kc.embedding <=> CAST(:queryVector AS vector) < :threshold
            ORDER BY kc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<KnowledgeCard> searchByVectorWithFilters(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("tag") String tag,
            @Param("categoryId") UUID categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("threshold") double threshold,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
    
    /**
     * 벡터 유사도 기반 지식 카드 검색 (특정 카드 제외 + 유사도 threshold 필터).
     * TIL 기반 리콜에서 원본 카드를 제외하고 threshold 이상인 카드 전체를 반환.
     * excludeIds는 반드시 1개 이상이어야 함 (빈 리스트 전달 시 SQL 오류).
     */
    @Query(value = """
            SELECT kc.card_id, kc.scrap_id, kc.category_id, kc.title, kc.summary,
                   kc.embedding, kc.created_at, kc.updated_at, kc.is_deleted
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
              AND kc.card_id NOT IN (:excludeIds)
              AND kc.embedding <=> CAST(:queryVector AS vector) < :threshold
            ORDER BY kc.embedding <=> CAST(:queryVector AS vector)
            """, nativeQuery = true)
    List<KnowledgeCard> searchByVectorExcludingWithThreshold(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("excludeIds") List<UUID> excludeIds,
            @Param("threshold") double threshold
    );

    /**
     * 카드 기준 유사 카드 조회용 벡터 검색.
     * threshold 미만의 가까운 카드만 DB에서 limit 개수만큼 조회한다.
     */
    @Query(value = """
            SELECT kc.card_id, kc.scrap_id, kc.category_id, kc.title, kc.summary,
                   kc.embedding, kc.created_at, kc.updated_at, kc.is_deleted
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
              AND kc.card_id NOT IN (:excludeIds)
              AND kc.embedding <=> CAST(:queryVector AS vector) < :threshold
            ORDER BY kc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeCard> searchSimilarCardsByVectorExcludingWithThreshold(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("excludeIds") List<UUID> excludeIds,
            @Param("threshold") double threshold,
            @Param("limit") int limit
    );

    /**
     * 태그·날짜 필터 조건에 맞는 전체 카드 수 조회.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM card_tags ct JOIN tags t ON ct.tag_id = t.tag_id
                  WHERE ct.card_id = kc.card_id AND t.tag_name = :tag
              ))
              AND (:fromDate IS NULL OR CAST(kc.created_at AS date) >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR CAST(kc.created_at AS date) <= CAST(:toDate AS date))
            """, nativeQuery = true)
    long countByVectorFilters(
            @Param("userId") UUID userId,
            @Param("tag") String tag,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * 태그·카테고리·날짜 필터 + 유사도 threshold 조건에 맞는 전체 카드 수 조회 (페이지네이션 totalCount용).
     * searchByVectorWithFilters와 동일 조건을 적용해야 hasNext 계산이 정확함.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND kc.embedding IS NOT NULL
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM card_tags ct JOIN tags t ON ct.tag_id = t.tag_id
                  WHERE ct.card_id = kc.card_id AND t.tag_name = :tag
              ))
              AND (:categoryId IS NULL OR kc.category_id = CAST(:categoryId AS uuid))
              AND (:fromDate IS NULL OR CAST(kc.created_at AS date) >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR CAST(kc.created_at AS date) <= CAST(:toDate AS date))
              AND kc.embedding <=> CAST(:queryVector AS vector) < :threshold
            """, nativeQuery = true)
    long countByVectorFiltersWithThreshold(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("tag") String tag,
            @Param("categoryId") UUID categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("threshold") double threshold
    );
    
    /**
     * 키워드 기반 지식 카드 검색 (title·summary ILIKE).
     * tag, categoryId, fromDate, toDate는 null 전달 시 필터 미적용.
     * Hybrid 검색에서 벡터 결과와 병합하기 위해 limit만 적용하고 offset은 서비스에서 처리한다.
     */
    @Query(value = """
            SELECT kc.card_id, kc.scrap_id, kc.category_id, kc.title, kc.summary,
                   kc.embedding, kc.created_at, kc.updated_at, kc.is_deleted
            FROM knowledge_cards kc
            JOIN scraps s ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND kc.is_deleted = false
              AND (kc.title ILIKE :pattern OR kc.summary ILIKE :pattern)
              AND (:tag IS NULL OR EXISTS (
                  SELECT 1 FROM card_tags ct JOIN tags t ON ct.tag_id = t.tag_id
                  WHERE ct.card_id = kc.card_id AND t.tag_name = :tag
              ))
              AND (:categoryId IS NULL OR kc.category_id = CAST(:categoryId AS uuid))
              AND (:fromDate IS NULL OR CAST(kc.created_at AS date) >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR CAST(kc.created_at AS date) <= CAST(:toDate AS date))
            ORDER BY kc.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeCard> searchByKeyword(
            @Param("pattern") String pattern,
            @Param("userId") UUID userId,
            @Param("tag") String tag,
            @Param("categoryId") UUID categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("limit") int limit
    );

    /**
     * 특정 기간에 스크랩이 생성된 지식카드의 소유자 userId 목록 조회 (중복 제거).
     * TIL 자동 생성 스케줄러에서 전날 활동한 사용자를 찾을 때 사용한다.
     */
    @Query("""
            SELECT DISTINCT s.user.userId
            FROM KnowledgeCard kc
            JOIN kc.scrap s
            WHERE s.createdAt >= :startAt AND s.createdAt < :endAt
            """)
    List<UUID> findDistinctUserIdsByScrapCreatedBetween(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
    
}
