package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.CardTag.CardTagId;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** 카드 태그 매핑 Repository */
public interface CardTagRepository extends JpaRepository<CardTag, CardTagId> {

    /**
     * 지식카드 목록 기준 태그 매핑 조회
     *
     * @param cards 지식카드 목록
     * @return 태그 매핑 목록
     */
    @Query("""
            SELECT ct
            FROM CardTag ct
            JOIN FETCH ct.tag
            WHERE ct.knowledgeCard IN :cards
            """)
    List<CardTag> findAllByKnowledgeCardInWithTag(List<KnowledgeCard> cards);
}
