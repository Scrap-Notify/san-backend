package com.san.api.domain.recall.repository;

import com.san.api.domain.recall.entity.RecallQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 리콜 퀴즈 Repository */
public interface RecallQuizRepository extends JpaRepository<RecallQuiz, UUID> {
}
