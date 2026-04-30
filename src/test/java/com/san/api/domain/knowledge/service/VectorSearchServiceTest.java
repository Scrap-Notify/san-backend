package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** VectorSearchService 비즈니스 로직 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private AiEmbeddingClient aiEmbeddingClient;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private UUID userId;
    private UUID otherUserId;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        user = buildUser(userId);
        otherUser = buildUser(otherUserId);
    }

    // ───────────────────────────────────────────────
    // findRelatedByCard
    // ───────────────────────────────────────────────

    @Test
    void findRelatedByCard_존재하지않는카드_예외() {
        UUID cardId = UUID.randomUUID();
        when(knowledgeCardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vectorSearchService.findRelatedByCard(cardId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
    }

    @Test
    void findRelatedByCard_타인카드접근_예외() {
        UUID cardId = UUID.randomUUID();
        KnowledgeCard card = buildCard(cardId, otherUser, new float[]{0.1f, 0.2f});
        when(knowledgeCardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> vectorSearchService.findRelatedByCard(cardId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
    }

    @Test
    void findRelatedByCard_임베딩없음_빈리스트반환() {
        UUID cardId = UUID.randomUUID();
        KnowledgeCard card = buildCard(cardId, user, null);
        when(knowledgeCardRepository.findById(cardId)).thenReturn(Optional.of(card));

        List<KnowledgeCard> result = vectorSearchService.findRelatedByCard(cardId, userId, 5);

        assertThat(result).isEmpty();
        verifyNoMoreInteractions(knowledgeCardRepository);
    }

    @Test
    void findRelatedByCard_자기자신제외하고_반환() {
        UUID cardId = UUID.randomUUID();
        KnowledgeCard baseCard = buildCard(cardId, user, new float[]{0.1f, 0.2f});
        KnowledgeCard related1 = buildCard(UUID.randomUUID(), user, new float[]{0.3f, 0.4f});
        KnowledgeCard related2 = buildCard(UUID.randomUUID(), user, new float[]{0.5f, 0.6f});

        when(knowledgeCardRepository.findById(cardId)).thenReturn(Optional.of(baseCard));
        // limit+1 개 조회 시 자기 자신 포함된 결과 반환
        when(knowledgeCardRepository.searchByVector(anyString(), eq(userId), eq(6), eq(0)))
                .thenReturn(List.of(baseCard, related1, related2));

        List<KnowledgeCard> result = vectorSearchService.findRelatedByCard(cardId, userId, 5);

        assertThat(result).doesNotContain(baseCard);
        assertThat(result).containsExactly(related1, related2);
    }

    // ───────────────────────────────────────────────
    // findRelatedByTil
    // ───────────────────────────────────────────────

    @Test
    void findRelatedByTil_존재하지않는TIL_예외() {
        UUID summaryId = UUID.randomUUID();
        when(dailySummaryRepository.findById(summaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vectorSearchService.findRelatedByTil(summaryId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_NOT_FOUND);
    }

    @Test
    void findRelatedByTil_타인TIL접근_예외() {
        UUID summaryId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, otherUser, new float[]{0.1f, 0.2f});
        when(dailySummaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> vectorSearchService.findRelatedByTil(summaryId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_ACCESS_DENIED);
    }

    @Test
    void findRelatedByTil_임베딩없음_빈리스트반환() {
        UUID summaryId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, user, null);
        when(dailySummaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));

        List<KnowledgeCard> result = vectorSearchService.findRelatedByTil(summaryId, userId, 5);

        assertThat(result).isEmpty();
        verifyNoInteractions(knowledgeCardRepository);
    }

    @Test
    void findRelatedByTil_원본카드없을때_excludeIds없이조회() {
        UUID summaryId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, user, new float[]{0.1f, 0.2f});
        KnowledgeCard related = buildCard(UUID.randomUUID(), user, new float[]{0.3f, 0.4f});

        when(dailySummaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));
        when(scrapRepository.findCardIdsByUserAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(knowledgeCardRepository.searchByVector(anyString(), eq(userId), eq(5), eq(0)))
                .thenReturn(List.of(related));

        List<KnowledgeCard> result = vectorSearchService.findRelatedByTil(summaryId, userId, 5);

        assertThat(result).containsExactly(related);
        verify(knowledgeCardRepository, never()).searchByVectorExcluding(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void findRelatedByTil_원본카드있을때_excludeIds로조회() {
        UUID summaryId = UUID.randomUUID();
        UUID sourceCardId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, user, new float[]{0.1f, 0.2f});
        KnowledgeCard related = buildCard(UUID.randomUUID(), user, new float[]{0.3f, 0.4f});

        when(dailySummaryRepository.findById(summaryId)).thenReturn(Optional.of(summary));
        when(scrapRepository.findCardIdsByUserAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(sourceCardId));
        when(knowledgeCardRepository.searchByVectorExcluding(anyString(), eq(userId), eq(List.of(sourceCardId)), eq(5), eq(0)))
                .thenReturn(List.of(related));

        List<KnowledgeCard> result = vectorSearchService.findRelatedByTil(summaryId, userId, 5);

        assertThat(result).containsExactly(related);
        verify(knowledgeCardRepository, never()).searchByVector(any(), any(), anyInt(), anyInt());
    }

    // ───────────────────────────────────────────────
    // 헬퍼 메서드
    // ───────────────────────────────────────────────

    private User buildUser(UUID id) {
        User u = User.builder()
                .username("user_" + id.toString().substring(0, 8))
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    private KnowledgeCard buildCard(UUID cardId, User owner, float[] embedding) {
        Scrap scrap = Scrap.builder()
                .user(owner)
                .sourceType(SourceType.TEXT)
                .rawContent("content")
                .build();
        Category category = Category.builder()
                .user(owner)
                .categoryName("테스트")
                .build();
        KnowledgeCard card = KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("테스트 카드")
                .summary("요약")
                .embedding(embedding)
                .build();
        ReflectionTestUtils.setField(card, "cardId", cardId);
        return card;
    }

    private DailySummary buildSummary(UUID summaryId, User owner, float[] embedding) {
        DailySummary summary = DailySummary.builder()
                .user(owner)
                .targetDate(LocalDate.now())
                .content("TIL 내용")
                .embedding(embedding)
                .build();
        ReflectionTestUtils.setField(summary, "summaryId", summaryId);
        return summary;
    }
}
