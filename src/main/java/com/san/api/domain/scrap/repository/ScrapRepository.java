package com.san.api.domain.scrap.repository;

import com.san.api.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 스크랩 엔티티 Repository */
public interface ScrapRepository extends JpaRepository<Scrap, UUID> {
}
