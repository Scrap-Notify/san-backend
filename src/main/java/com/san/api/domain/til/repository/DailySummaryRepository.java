package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 매일의 요약 Repository */
public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {
}
