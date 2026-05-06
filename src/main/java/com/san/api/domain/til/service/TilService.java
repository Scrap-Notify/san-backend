package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.service.VectorSearchService;
import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.dto.response.TilRecallCardsResponse;
import com.san.api.domain.til.dto.response.TilResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** TIL 생성 작업 등록 및 조회 Service */
@Service
@RequiredArgsConstructor
public class TilService {

    private final DailySummaryRepository dailySummaryRepository;
    private final DailySummaryService dailySummaryService;
    private final AsyncJobManager asyncJobManager;
    private final VectorSearchService vectorSearchService;
    private final CardTagRepository cardTagRepository;

    /**
     * TIL 생성 비동기 작업 등록
     *
     * @param userId  로그인 사용자 ID
     * @param request TIL 생성 작업 등록 요청
     * @return 등록된 TIL 생성 작업 응답
     */
    public TilGenerationJobResponse requestGeneration(UUID userId, TilGenerateRequest request) {
        DailySummary summary = dailySummaryService.createSummary(userId, request.targetDate());
        UUID jobId = asyncJobManager.enqueue(JobType.TIL_GENERATION, summary.getSummaryId());

        return new TilGenerationJobResponse(
                summary.getSummaryId(),
                jobId,
                summary.getTargetDate()
        );
    }

    /**
     * 날짜 기준 TIL 조회
     *
     * @param userId     로그인 사용자 ID
     * @param targetDate 조회 대상 날짜
     * @return 날짜 기준 TIL 조회 응답
     */
    @Transactional(readOnly = true)
    public List<TilResponse> getTil(UUID userId, LocalDate targetDate) {
        return dailySummaryRepository.findAllByUser_UserIdAndTargetDateOrderByCreatedAtDesc(userId, targetDate)
                .stream()
                .map(TilResponse::from)
                .toList();
    }

    /**
     * TIL 기반 리콜 카드 조회.
     * TIL 임베딩과 유사한 카드를 threshold 기준으로 전체 반환한다.
     *
     * @param summaryId TIL ID
     * @param userId    로그인 사용자 ID
     * @return 리콜 카드 목록
     */
    @Transactional(readOnly = true)
    public TilRecallCardsResponse getRecallCards(UUID summaryId, UUID userId) {
        List<KnowledgeCard> cards = vectorSearchService.findRelatedByTil(summaryId, userId);

        if (cards.isEmpty()) {
            return new TilRecallCardsResponse(List.of());
        }

        Map<UUID, List<CardTag>> tagsByCardId = cardTagRepository.findAllByKnowledgeCardInWithTag(cards)
                .stream()
                .collect(Collectors.groupingBy(ct -> ct.getKnowledgeCard().getCardId()));

        List<KnowledgeCardResponse> responses = cards.stream()
                .map(card -> KnowledgeCardResponse.from(
                        card,
                        tagsByCardId.getOrDefault(card.getCardId(), List.of())
                ))
                .toList();

        return new TilRecallCardsResponse(responses);
    }
}
