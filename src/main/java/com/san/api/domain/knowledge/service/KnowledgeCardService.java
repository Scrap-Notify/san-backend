package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisJobResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisResultResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardListResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.enums.JobTypeEnum;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
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
    private final CardTagRepository cardTagRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * 저장된 수집 원본 기반 지식카드 AI 분석 작업 등록
     *
     * @param userId 로그인 사용자 ID
     * @param request 지식카드 AI 분석 요청
     * @return 등록된 비동기 작업 응답
     */
    @Transactional
    public KnowledgeCardAnalysisJobResponse createCard(UUID userId, KnowledgeCardCreateRequest request) {
        Scrap scrap = scrapRepository.findById(request.scrapId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateScrapOwner(scrap, userId);
        validateNotCreated(request.scrapId());

        UUID jobId = asyncJobManager.enqueue(JobTypeEnum.CARD_ANALYSIS, request.scrapId());
        return new KnowledgeCardAnalysisJobResponse(jobId);
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
     * 지식카드 AI 분석 작업 결과 조회
     *
     * @param userId 로그인 사용자 ID
     * @param jobId 분석 작업 ID
     * @return 지식카드 AI 분석 작업 결과 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardAnalysisResultResponse getAnalysisResult(UUID userId, UUID jobId) {
        AsyncJob job = asyncJobManager.getJob(jobId);
        validateCardAnalysisJob(job);

        Scrap scrap = scrapRepository.findById(job.getTargetId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        validateScrapOwner(scrap, userId);

        KnowledgeCardResponse card = switch (job.getStatus()) {
            case COMPLETED -> getCreatedCardResponse(job.getTargetId());
            case PENDING, PROCESSING, FAILED -> null;
        };

        return new KnowledgeCardAnalysisResultResponse(
                job.getJobId(),
                job.getStatus(),
                card,
                job.getErrorMessage()
        );
    }

    /** 지식카드 분석 작업 여부 검증 */
    private void validateCardAnalysisJob(AsyncJob job) {
        if (job.getJobType() != JobTypeEnum.CARD_ANALYSIS) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    /** 수집 원본 기준 생성된 지식카드 응답 조회 */
    private KnowledgeCardResponse getCreatedCardResponse(UUID scrapId) {
        KnowledgeCard card = knowledgeCardRepository.findByScrapIdWithCategory(scrapId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));
        List<CardTag> cardTags = cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card));

        return KnowledgeCardResponse.from(card, cardTags);
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
}
