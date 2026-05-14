package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.response.KnowledgeCardDetailResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeCardServiceTest {

    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private CardTagRepository cardTagRepository;
    @Mock
    private AsyncJobManager asyncJobManager;
    @Mock
    private VectorSearchService vectorSearchService;

    @InjectMocks
    private KnowledgeCardService knowledgeCardService;

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

    @Test
    void getCardDetail_returnsDetailResponse() {
        KnowledgeCard card = buildCard(user);
        CardTag springTag = new CardTag(card, Tag.builder().tagName("Spring").build());
        CardTag javaTag = new CardTag(card, Tag.builder().tagName("Java").build());

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card)))
                .thenReturn(List.of(springTag, javaTag));

        KnowledgeCardDetailResponse response = knowledgeCardService.getCardDetail(userId, card.getCardId());

        assertThat(response.title()).isEqualTo("지식카드 제목");
        assertThat(response.categoryId()).isEqualTo(card.getCategory().getCategoryId());
        assertThat(response.categoryName()).isEqualTo("Backend");
        assertThat(response.rawContent()).isEqualTo("원본 내용");
        assertThat(response.refinedContent()).isEqualTo("정제된 원본 내용");
        assertThat(response.summary()).isEqualTo("3줄 요약");
        assertThat(response.tags()).containsExactly("Spring", "Java");
    }

    @Test
    void getCardDetail_throwsCardNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(userId, cardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void getCardDetail_throwsCardAccessDeniedWhenUserIsNotOwner() {
        KnowledgeCard card = buildCard(otherUser);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(userId, card.getCardId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void getCardIdByScrap_throwsScrapAccessDeniedWhenUserIsNotOwner() {
        Scrap scrap = buildScrap(otherUser);
        when(scrapRepository.findById(scrap.getScrapId())).thenReturn(Optional.of(scrap));

        assertThatThrownBy(() -> knowledgeCardService.getCardIdByScrap(userId, scrap.getScrapId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScrapErrorCode.SCRAP_ACCESS_DENIED);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_deletesCard() {
        KnowledgeCard card = buildCard(user);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        knowledgeCardService.deleteCard(userId, card.getCardId());

        assertThat(card.isDeleted()).isTrue();
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_throwsCardNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeCardService.deleteCard(userId, cardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_throwsCardAccessDeniedWhenUserIsNotOwner() {
        KnowledgeCard card = buildCard(otherUser);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> knowledgeCardService.deleteCard(userId, card.getCardId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
        assertThat(card.isDeleted()).isFalse();
        verifyNoInteractions(cardTagRepository);
    }

    private User buildUser(UUID id) {
        User user = User.builder()
                .username("user_" + id.toString().substring(0, 8))
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    private KnowledgeCard buildCard(User owner) {
        Scrap scrap = buildScrap(owner);
        Category category = Category.builder()
                .user(owner)
                .categoryName("Backend")
                .build();
        return KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("지식카드 제목")
                .summary("3줄 요약")
                .embedding(new float[]{0.1f, 0.2f})
                .build();
    }

    private Scrap buildScrap(User owner) {
        return Scrap.builder()
                .user(owner)
                .sourceType(SourceType.TEXT)
                .rawContent("원본 내용")
                .refinedContent("정제된 원본 내용")
                .build();
    }
}
