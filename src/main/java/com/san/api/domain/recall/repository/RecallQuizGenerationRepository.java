package com.san.api.domain.recall.repository;

import com.san.api.domain.recall.entity.RecallQuizGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** 리콜 퀴즈 생성 Repository */
public interface RecallQuizGenerationRepository extends JpaRepository<RecallQuizGeneration, UUID> {

    /** 리콜 퀴즈 생성 작업 처리용 사용자 포함 조회 */
    @Query("""
            select generation
            from RecallQuizGeneration generation
            join fetch generation.user
            where generation.generationId = :generationId
            """)
    Optional<RecallQuizGeneration> findByGenerationIdWithUser(@Param("generationId") UUID generationId);
}
