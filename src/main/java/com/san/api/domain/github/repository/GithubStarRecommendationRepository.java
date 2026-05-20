package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubStarRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** GitHub Star 추천 후보 Repository */
public interface GithubStarRecommendationRepository extends JpaRepository<GithubStarRecommendation, UUID> {

    // 사용자 추천 후보 존재 여부 조회
    boolean existsByUser_UserId(UUID userId);

    // 사용자 추천 후보 목록 조회
    List<GithubStarRecommendation> findAllByUser_UserIdOrderByCreatedAtDesc(UUID userId);

    // 비동기 작업 추천 후보 목록 조회
    List<GithubStarRecommendation> findAllByJobIdOrderByCreatedAtAsc(UUID jobId);

    // 사용자 추천 후보 단건 조회
    Optional<GithubStarRecommendation> findByGithubStarRecommendationIdAndUser_UserId(
            UUID githubStarRecommendationId,
            UUID userId
    );
}
