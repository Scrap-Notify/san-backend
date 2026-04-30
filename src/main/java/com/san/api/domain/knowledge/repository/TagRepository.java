package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 태그 Repository */
public interface TagRepository extends JpaRepository<Tag, UUID> {
}
