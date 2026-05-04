package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardListResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.repository.TagRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.external.ai.client.AiAnalysisClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 지식카드 Service */
@Service
@RequiredArgsConstructor
public class KnowledgeCardService {

    private final ScrapRepository scrapRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CardTagRepository cardTagRepository;
    private final AiAnalysisClient aiAnalysisClient;

    /**
     * 저장된 수집 원본 기반 지식카드 생성
     *
     * @param userId 로그인 사용자 ID
     * @param request 지식카드 생성 요청
     * @return 생성된 지식카드 응답
     */
    @Transactional
    public KnowledgeCardResponse createCard(UUID userId, KnowledgeCardCreateRequest request) {
        Scrap scrap = scrapRepository.findById(request.scrapId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateScrapOwner(scrap, userId);
        validateNotCreated(request.scrapId());

        AiAnalyzeResponse analysis = aiAnalysisClient.analyze(toAnalyzeRequest(scrap));
        Category category = findOrCreateCategory(userId, scrap, analysis.category());

        KnowledgeCard card = knowledgeCardRepository.save(KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title(analysis.title().trim())
                .summary(analysis.summary().trim())
                .embedding(analysis.embedding())
                .build());

        List<CardTag> cardTags = saveTags(card, analysis.tags());

        return KnowledgeCardResponse.from(card, cardTags);
    }

    /**
     * 로그인 사용자 기준 지식카드 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @return 지식카드 목록 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardListResponse getCards(UUID userId) {
        List<KnowledgeCard> cards = knowledgeCardRepository.findByScrap_User_UserIdOrderByCreatedAtDesc(userId);
        if (cards.isEmpty()) {
            return new KnowledgeCardListResponse(List.of());
        }

        Map<UUID, List<CardTag>> cardTagsByCardId = cardTagRepository.findAllByKnowledgeCardInWithTag(cards).stream()
                .collect(Collectors.groupingBy(cardTag -> cardTag.getKnowledgeCard().getCardId()));

        List<KnowledgeCardResponse> responses = cards.stream()
                .map(card -> KnowledgeCardResponse.from(
                        card,
                        cardTagsByCardId.getOrDefault(card.getCardId(), List.of())
                ))
                .toList();

        return new KnowledgeCardListResponse(responses);
    }

    /**
     * 원본 소유자 검증
     *
     * @param scrap 수집 원본
     * @param userId 로그인 사용자 ID
     */
    private void validateScrapOwner(Scrap scrap, UUID userId) {
        if (!scrap.getUser().getUserId().equals(userId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    /**
     * 동일 원본 기반 지식카드 생성 여부 검증
     *
     * @param scrapId 수집 원본 ID
     */
    private void validateNotCreated(UUID scrapId) {
        if (knowledgeCardRepository.existsByScrap_ScrapId(scrapId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ALREADY_EXISTS);
        }
    }

    /**
     * 수집 원본을 AI 분석 요청으로 변환
     *
     * @param scrap 수집 원본
     * @return AI 분석 요청
     */
    private AiAnalyzeRequest toAnalyzeRequest(Scrap scrap) {
        String content = resolveContent(scrap);
        if (isBlank(content)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        return new AiAnalyzeRequest(toInputType(scrap.getSourceType()), content.trim());
    }

    /**
     * SourceType 기준 AI input_type 변환
     *
     * @param sourceType 수집 원본 유형
     * @return AI 서버 input_type
     */
    private String toInputType(SourceType sourceType) {
        return switch (sourceType) {
            case LINK -> "url";
            case TEXT -> "text";
            case IMAGE -> "image";
        };
    }

    /**
     * AI 서버에 전달할 원본 content 선택
     *
     * @param scrap 수집 원본
     * @return AI 분석 대상 content
     */
    private String resolveContent(Scrap scrap) {
        return switch (scrap.getSourceType()) {
            case LINK -> firstNotBlank(scrap.getSourceUrl(), scrap.getRawContent());
            case TEXT -> scrap.getRawContent();
            case IMAGE -> firstNotBlank(scrap.getImageUrl(), scrap.getRawContent());
        };
    }

    /**
     * 카테고리 조회 또는 생성
     *
     * @param userId 로그인 사용자 ID
     * @param scrap 수집 원본
     * @param categoryName AI 분석 카테고리명
     * @return 카테고리
     */
    private Category findOrCreateCategory(UUID userId, Scrap scrap, String categoryName) {
        String normalizedName = categoryName.trim();
        return categoryRepository.findByUser_UserIdAndCategoryName(userId, normalizedName)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .user(scrap.getUser())
                        .categoryName(normalizedName)
                        .build()));
    }

    /**
     * 태그 저장 및 지식카드 연결
     *
     * @param card 지식카드
     * @param tagNames AI 분석 태그명 목록
     * @return 지식카드 태그 매핑 목록
     */
    private List<CardTag> saveTags(KnowledgeCard card, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        return tagNames.stream()
                .filter(tagName -> !isBlank(tagName))
                .map(String::trim)
                .distinct()
                .map(tagName -> findOrCreateTag(card, tagName))
                .toList();
    }

    /**
     * 태그 조회 또는 생성 후 지식카드 연결
     *
     * @param card 지식카드
     * @param tagName 태그명
     * @return 지식카드 태그 매핑
     */
    private CardTag findOrCreateTag(KnowledgeCard card, String tagName) {
        Tag tag = tagRepository.findByTagName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .tagName(tagName)
                        .build()));

        return cardTagRepository.save(new CardTag(card, tag));
    }

    /**
     * 첫 번째 유효 문자열 선택
     *
     * @param values 후보 값 목록
     * @return 첫 번째 유효 문자열 또는 null
     */
    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 빈 문자열 여부
     *
     * @param value 검증 대상 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
