package com.san.api.domain.github.service;

import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationListResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 작업 Service */
@Service
@RequiredArgsConstructor
public class GithubStarRecommendationJobService {

    private final GithubStarRecommendationRepository githubStarRecommendationRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * GitHub Star 추천 작업 요청
     *
     * @param userId 사용자 ID
     * @return 기존 추천 후보 또는 등록된 작업 ID
     */
    @Transactional
    public GithubStarRecommendationJobResponse requestRecommendation(UUID userId) {
        List<GithubStarRecommendation> recommendations = findUserRecommendations(userId);
        if (!recommendations.isEmpty()) {
            return GithubStarRecommendationJobResponse.alreadyRecommended(recommendations);
        }

        UUID jobId = asyncJobManager.enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
        return GithubStarRecommendationJobResponse.created(jobId);
    }

    /**
     * 사용자 GitHub Star 추천 후보 목록 조회
     *
     * @param userId 사용자 ID
     * @return GitHub Star 추천 후보 목록 응답
     */
    @Transactional(readOnly = true)
    public GithubStarRecommendationListResponse getRecommendations(UUID userId) {
        return GithubStarRecommendationListResponse.from(findUserRecommendations(userId));
    }

    private List<GithubStarRecommendation> findUserRecommendations(UUID userId) {
        return githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
    }
}
