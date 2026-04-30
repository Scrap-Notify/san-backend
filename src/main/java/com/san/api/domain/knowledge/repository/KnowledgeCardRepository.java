package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 지식 카드 Repository */
public interface KnowledgeCardRepository extends JpaRepository<KnowledgeCard, UUID> {
}
