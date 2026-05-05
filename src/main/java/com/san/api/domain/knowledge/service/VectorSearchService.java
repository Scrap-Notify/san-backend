package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.response.SearchResponse;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * pgvector 기반 유사도 검색 서비스.
 * 자연어 통합 검색과 카드 기반 연관 추천 두 유즈케이스를 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VectorSearchService {

    // pgvector <=> 코사인 거리 기준. 0.3 미만 = 유사도 0.7 초과
    private static final double RECALL_THRESHOLD = 0.3;

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final ScrapRepository scrapRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final AiEmbeddingClient aiEmbeddingClient;

    /**
     * 자연어 검색어 기반 지식 카드 유사도 검색.
     * 검색어를 AI 서버에서 벡터로 변환한 뒤 pgvector로 검색한다.
     * tag, fromDate, toDate는 null 전달 시 필터 미적용.
     *
     * @param keyword  사용자 검색어
     * @param userId   요청자 ID (권한 필터)
     * @param tag      태그명 필터 (null = 미적용)
     * @param fromDate 시작일 필터 (null = 미적용)
     * @param toDate   종료일 필터 (null = 미적용)
     * @param page     페이지 번호 (0-based)
     * @param size     페이지 크기
     * @return 검색 결과 및 페이지 정보 (totalCount, hasNext 포함)
     */
    public SearchResponse search(String keyword, UUID userId, String tag,
                                 LocalDate fromDate, LocalDate toDate, int page, int size) {
        // TODO: AI 서버 실행 환경에서 E2E 동작 검증 필요
        float[] vector = aiEmbeddingClient.embed(keyword);
        String queryVector = toVectorString(vector);

        int offset = page * size;
        List<KnowledgeCard> cards = knowledgeCardRepository.searchByVectorWithFilters(
                queryVector, userId, tag, fromDate, toDate, size, offset);
        long totalCount = knowledgeCardRepository.countByVectorFilters(userId, tag, fromDate, toDate);

        return SearchResponse.of(keyword, page, size, totalCount, cards);
    }

    /**
     * 카드 기반 연관 카드 추천.
     * 기준 카드의 임베딩을 DB에서 직접 조회하여 AI 서버 호출 없이 검색한다.
     * 결과에서 기준 카드 자신은 제외된다.
     *
     * @param cardId 기준 카드 ID
     * @param userId 요청자 ID (권한 필터)
     * @param limit  최대 반환 개수
     */
    public List<KnowledgeCard> findRelatedByCard(UUID cardId, UUID userId, int limit) {
        KnowledgeCard baseCard = knowledgeCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));

        if (!baseCard.getScrap().getUser().getUserId().equals(userId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ACCESS_DENIED);
        }

        if (baseCard.getEmbedding() == null) {
            return List.of();
        }

        String queryVector = toVectorString(baseCard.getEmbedding());
        return knowledgeCardRepository.searchByVector(queryVector, userId, limit + 1, 0)
                .stream()
                .filter(card -> !card.getCardId().equals(cardId))
                .limit(limit)
                .toList();
    }

    /**
     * TIL 기반 리콜 카드 추천.
     * TIL의 임베딩을 기준으로 유사 카드를 검색하며, 해당 TIL 생성에 사용된 원본 카드는 제외한다.
     * RECALL_THRESHOLD 미만의 코사인 거리를 가진 카드를 전체 반환한다.
     *
     * @param summaryId TIL ID
     * @param userId    요청자 ID (권한 필터)
     */
    public List<KnowledgeCard> findRelatedByTil(UUID summaryId, UUID userId) {
        DailySummary summary = dailySummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));

        if (!summary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(TilErrorCode.SUMMARY_ACCESS_DENIED);
        }

        if (summary.getEmbedding() == null) {
            return List.of();
        }

        String queryVector = toVectorString(summary.getEmbedding());
        List<UUID> excludeIds = scrapRepository.findCardIdsByUserAndDate(userId, summary.getTargetDate());

        return knowledgeCardRepository.searchByVectorExcludingWithThreshold(
                queryVector, userId, excludeIds, RECALL_THRESHOLD);
    }

    // float[] → "[0.1, 0.2, ...]" 형식 변환 (pgvector CAST용)
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
