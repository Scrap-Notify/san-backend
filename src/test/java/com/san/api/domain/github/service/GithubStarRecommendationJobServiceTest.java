package com.san.api.domain.github.service;

import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationListResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationJobServiceTest {

    private GithubStarRecommendationRepository githubStarRecommendationRepository;
    private AsyncJobManager asyncJobManager;
    private GithubStarRecommendationJobService service;

    @BeforeEach
    void setUp() {
        githubStarRecommendationRepository = mock(GithubStarRecommendationRepository.class);
        asyncJobManager = mock(AsyncJobManager.class);
        service = new GithubStarRecommendationJobService(githubStarRecommendationRepository, asyncJobManager);
    }

    @Test
    void requestRecommendation_enqueuesJobWhenRecommendationDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId)).thenReturn(jobId);

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.alreadyRecommended()).isFalse();
        assertThat(response.recommendations()).isEmpty();
        verify(asyncJobManager).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void requestRecommendation_returnsExistingRecommendationsWithoutEnqueue() {
        UUID userId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(recommendation));

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isNull();
        assertThat(response.alreadyRecommended()).isTrue();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).url()).isEqualTo("https://example.com");
        verify(asyncJobManager, never()).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void getRecommendations_returnsUserRecommendations() {
        UUID userId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(recommendation));

        GithubStarRecommendationListResponse response = service.getRecommendations(userId);

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).title()).isEqualTo("title");
    }

    private GithubStarRecommendation buildRecommendation() {
        User user = User.builder()
                .username("user@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        return new GithubStarRecommendation(
                user,
                UUID.randomUUID(),
                "https://example.com",
                "title",
                "summary",
                "{\"title\":\"title\"}"
        );
    }
}
