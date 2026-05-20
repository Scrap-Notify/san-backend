package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.dto.response.GithubStarRecommendationCollectResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.repository.TagRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.scrap.service.ScrapContentHashPolicy;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationCollectServiceTest {

    private GithubStarRecommendationRepository githubStarRecommendationRepository;
    private ScrapRepository scrapRepository;
    private KnowledgeCardRepository knowledgeCardRepository;
    private CategoryRepository categoryRepository;
    private TagRepository tagRepository;
    private CardTagRepository cardTagRepository;
    private GithubStarRecommendationCollectService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        githubStarRecommendationRepository = mock(GithubStarRecommendationRepository.class);
        scrapRepository = mock(ScrapRepository.class);
        knowledgeCardRepository = mock(KnowledgeCardRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        tagRepository = mock(TagRepository.class);
        cardTagRepository = mock(CardTagRepository.class);
        objectMapper = new ObjectMapper();
        service = new GithubStarRecommendationCollectService(
                githubStarRecommendationRepository,
                scrapRepository,
                knowledgeCardRepository,
                categoryRepository,
                tagRepository,
                cardTagRepository,
                new ScrapContentHashPolicy(),
                objectMapper
        );
    }

    @Test
    void collect_createsScrapAndKnowledgeCardFromSavedAnalysisResult() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        User user = buildUser(userId);
        GithubStarRecommendation recommendation = buildRecommendation(user, recommendationId);
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                "Analyzed title",
                "Analyzed summary",
                List.of("Spring"),
                "Backend",
                new float[]{0.1f}
        );
        ReflectionTestUtils.setField(recommendation, "analysisResult", objectMapper.writeValueAsString(analysis));
        Category category = Category.builder()
                .user(user)
                .categoryName("Backend")
                .build();

        when(githubStarRecommendationRepository.findByGithubStarRecommendationIdAndUser_UserId(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeCardRepository.findByScrapIdWithCategory(any())).thenReturn(Optional.empty());
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Backend")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findByTagName("Spring")).thenReturn(Optional.empty());

        GithubStarRecommendationCollectResponse response = service.collect(userId, recommendationId);

        assertThat(response.recommendationId()).isEqualTo(recommendationId);
        assertThat(response.scrapId()).isNotNull();
        assertThat(response.cardId()).isNotNull();
        assertThat(response.collected()).isTrue();
        assertThat(recommendation.isCollected()).isTrue();
        assertThat(recommendation.getCollectedTargetId()).isEqualTo(response.cardId());
        verify(cardTagRepository).save(any());
    }

    @Test
    void collect_throwsWhenRecommendationAlreadyCollected() {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation(buildUser(userId), recommendationId);
        recommendation.markCollected(UUID.randomUUID());

        when(githubStarRecommendationRepository.findByGithubStarRecommendationIdAndUser_UserId(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.collect(userId, recommendationId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    void collect_throwsWhenRecommendationDoesNotExistForUser() {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        when(githubStarRecommendationRepository.findByGithubStarRecommendationIdAndUser_UserId(recommendationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.collect(userId, recommendationId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("user@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private GithubStarRecommendation buildRecommendation(User user, UUID recommendationId) {
        GithubStarRecommendation recommendation = new GithubStarRecommendation(
                user,
                UUID.randomUUID(),
                "https://example.com",
                "title",
                "summary",
                "{\"title\":\"title\",\"summary\":\"summary\",\"tags\":[],\"category\":\"Backend\",\"embedding\":[0.1]}"
        );
        ReflectionTestUtils.setField(recommendation, "githubStarRecommendationId", recommendationId);
        return recommendation;
    }
}
