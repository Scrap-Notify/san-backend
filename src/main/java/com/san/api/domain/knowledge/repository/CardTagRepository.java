package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.CardTag.CardTagId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 카드 태그 매핑 Repository */
public interface CardTagRepository extends JpaRepository<CardTag, CardTagId> {
}
