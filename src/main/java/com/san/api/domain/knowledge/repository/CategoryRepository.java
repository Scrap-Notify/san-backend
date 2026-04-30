package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 카테고리 Repository */
public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
