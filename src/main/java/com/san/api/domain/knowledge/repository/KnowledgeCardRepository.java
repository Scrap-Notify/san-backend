package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** 지식 카드 Repository */
public interface KnowledgeCardRepository extends JpaRepository<KnowledgeCard, UUID> {

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
     * 벡터 유사도 기반 지식 카드 검색 (특정 카드 제외).
     * TIL 기반 리콜에서 원본 카드를 제외할 때 사용.
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
            ORDER BY kc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<KnowledgeCard> searchByVectorExcluding(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("excludeIds") List<UUID> excludeIds,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
