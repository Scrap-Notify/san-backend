package com.san.api.domain.recall.repository;

import com.san.api.domain.recall.entity.RecallQuizGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 리콜 퀴즈 생성 Repository */
public interface RecallQuizGenerationRepository extends JpaRepository<RecallQuizGeneration, UUID> {
}
