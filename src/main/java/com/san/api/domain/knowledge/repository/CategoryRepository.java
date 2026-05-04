package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** 카테고리 Repository */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 사용자와 카테고리명 기준 카테고리 조회
    Optional<Category> findByUser_UserIdAndCategoryName(UUID userId, String categoryName);
}
