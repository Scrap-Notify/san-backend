package com.san.api.domain.recall.service;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.dto.response.RecallQuizGenerateResponse;
import com.san.api.domain.recall.dto.response.RecallQuizGenerationJobResponse;
import com.san.api.domain.recall.dto.response.RecallQuizListResponse;
import com.san.api.domain.recall.dto.response.RecallQuizResponse;
import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.domain.recall.repository.RecallQuizRepository;
import com.san.api.domain.recall.service.RecallQuizSourceService.RecallQuizSourceResult;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import com.san.api.global.external.ai.client.AiQuizClient;
import com.san.api.global.external.ai.dto.request.AiQuizContentRequest;
import com.san.api.global.external.ai.dto.request.AiQuizRequest;
import com.san.api.global.external.ai.dto.response.AiOxQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiOxQuizResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** 리콜 퀴즈 생성 Service */
@Service
@RequiredArgsConstructor
public class RecallQuizGenerationService {

    private static final String SHORT_ANSWER_AI_QUIZ_TYPE = "short_answer";
    private static final String OX_AI_QUIZ_TYPE = "ox";
    private static final String O_ANSWER = "O";
    private static final String X_ANSWER = "X";

    private final RecallQuizSourceService recallQuizSourceService;
    private final RecallQuizRepository recallQuizRepository;
    private final RecallQuizGenerationRepository recallQuizGenerationRepository;
    private final RecallQuizPersistenceService recallQuizPersistenceService;
    private final UserRepository userRepository;
    private final AsyncJobManager asyncJobManager;
    private final AiQuizClient aiQuizClient;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 리콜 퀴즈 생성 작업 등록
     *
     * @param userId 사용자 ID
     * @param request 리콜 퀴즈 생성 요청
     * @return 등록된 리콜 퀴즈 생성 작업 응답
     */
    public RecallQuizGenerationJobResponse requestGeneration(UUID userId, RecallQuizGenerateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        RecallQuizGeneration generation = recallQuizGenerationRepository.save(
                RecallQuizGeneration.builder()
                        .user(user)
                        .targetDate(request.targetDate())
                        .quizType(request.quizType())
                        .build()
        );
        UUID quizJobId = asyncJobManager.enqueue(JobType.RECALL_QUIZ_GENERATION, generation.getGenerationId());

        return new RecallQuizGenerationJobResponse(
                generation.getGenerationId(),
                quizJobId,
                generation.getTargetDate(),
                generation.getQuizType()
        );
    }

    /**
     * 날짜와 유형 기준 리콜 퀴즈 조회
     *
     * @param userId 사용자 ID
     * @param targetDate 조회 대상 날짜
     * @param quizType 리콜 퀴즈 유형
     * @return 리콜 퀴즈 목록 응답
     */
    @Transactional(readOnly = true)
    public RecallQuizListResponse getQuizzes(UUID userId, LocalDate targetDate, RecallQuizType quizType) {
        List<RecallQuiz> quizzes = recallQuizRepository
                .findAllByUser_UserIdAndDailySummary_TargetDateAndQuizTypeOrderByCreatedAtAsc(
                        userId,
                        targetDate,
                        quizType
                );

        return new RecallQuizListResponse(
                targetDate,
                quizType,
                quizzes.stream()
                        .map(RecallQuizResponse::from)
                        .toList()
        );
    }

    /**
     * 날짜 기반 리콜 퀴즈 생성
     *
     * @param userId 사용자 ID
     * @param request 리콜 퀴즈 생성 요청
     * @return 리콜 퀴즈 생성 응답
     */
    @Transactional
    public RecallQuizGenerateResponse generate(UUID userId, RecallQuizGenerateRequest request) {
        RecallQuizSourceResult source = recallQuizSourceService.findSources(userId, request.targetDate());
        DailySummary summary = source.dailySummary();

        List<RecallQuiz> existingQuizzes = findExistingQuizzes(userId, summary, request.quizType());
        if (!existingQuizzes.isEmpty()) {
            return toResponse(summary, request.quizType(), existingQuizzes);
        }

        AiQuizRequest aiRequest = new AiQuizRequest(
                source.sourceCards().stream()
                        .map(this::toAiQuizContentRequest)
                        .toList(),
                toAiQuizType(request.quizType())
        );

        List<RecallQuiz> quizzes = switch (request.quizType()) {
            case SHORT_ANSWER -> createShortAnswerQuizzes(summary, source.sourceCards(), aiRequest);
            case OX -> createOxQuizzes(summary, source.sourceCards(), aiRequest);
        };

        try {
            return toResponse(summary, request.quizType(), recallQuizPersistenceService.saveQuizzes(quizzes));
        } catch (DataIntegrityViolationException e) {
            List<RecallQuiz> conflictQuizzes = findExistingQuizzes(userId, summary, request.quizType());
            if (!conflictQuizzes.isEmpty()) {
                return toResponse(summary, request.quizType(), conflictQuizzes);
            }
            throw e;
        }
    }

    /** 기존 리콜 퀴즈 조회 */
    private List<RecallQuiz> findExistingQuizzes(UUID userId, DailySummary summary, RecallQuizType quizType) {
        return recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                quizType
        );
    }

    /** 단답형 퀴즈 생성 */
    private List<RecallQuiz> createShortAnswerQuizzes(
            DailySummary summary,
            List<KnowledgeCard> sourceCards,
            AiQuizRequest aiRequest
    ) {
        AiShortAnswerQuizResponse aiResponse = aiQuizClient.generateShortAnswerQuiz(aiRequest);
        return createShortAnswerQuizzes(summary, sourceCards, aiResponse.questions());
    }

    /** OX 퀴즈 생성 */
    private List<RecallQuiz> createOxQuizzes(
            DailySummary summary,
            List<KnowledgeCard> sourceCards,
            AiQuizRequest aiRequest
    ) {
        AiOxQuizResponse aiResponse = aiQuizClient.generateOxQuiz(aiRequest);
        return createOxQuizzes(summary, sourceCards, aiResponse.questions());
    }

    /** 단답형 AI 응답 저장 엔티티 변환 */
    private List<RecallQuiz> createShortAnswerQuizzes(
            DailySummary summary,
            List<KnowledgeCard> sourceCards,
            List<AiShortAnswerQuizQuestionResponse> questions
    ) {
        return IntStream.range(0, sourceCards.size())
                .mapToObj(index -> {
                    KnowledgeCard card = sourceCards.get(index);
                    AiShortAnswerQuizQuestionResponse question = questions.get(index);
                    return RecallQuiz.builder()
                            .dailySummary(summary)
                            .scrap(card.getScrap())
                            .quizType(RecallQuizType.SHORT_ANSWER)
                            .question(question.question())
                            .answer(question.answer())
                            .explanation(question.explanation())
                            .build();
                })
                .toList();
    }

    /** OX AI 응답 저장 엔티티 변환 */
    private List<RecallQuiz> createOxQuizzes(
            DailySummary summary,
            List<KnowledgeCard> sourceCards,
            List<AiOxQuizQuestionResponse> questions
    ) {
        return IntStream.range(0, sourceCards.size())
                .mapToObj(index -> {
                    KnowledgeCard card = sourceCards.get(index);
                    AiOxQuizQuestionResponse question = questions.get(index);
                    return RecallQuiz.builder()
                            .dailySummary(summary)
                            .scrap(card.getScrap())
                            .quizType(RecallQuizType.OX)
                            .question(question.statement())
                            .answer(toOxAnswer(question.isCorrect()))
                            .explanation(question.explanation())
                            .build();
                })
                .toList();
    }

    /** TIL 원본 기준 AI 요청 content 변환 */
    private AiQuizContentRequest toAiQuizContentRequest(KnowledgeCard card) {
        Scrap scrap = card.getScrap();
        String content = resolveContent(scrap);
        if (isBlank(content)) {
            throw new BusinessException(RecallErrorCode.INVALID_RECALL_SOURCE_CONTENT);
        }

        return new AiQuizContentRequest(toInputType(scrap.getSourceType()), content.trim());
    }

    /** SourceType을 AI input_type으로 변환 */
    private String toInputType(SourceType sourceType) {
        return switch (sourceType) {
            case LINK -> "url";
            case TEXT -> "text";
            case IMAGE -> "image";
        };
    }

    /** 원본 타입별 AI 요청 content 선택 */
    private String resolveContent(Scrap scrap) {
        return switch (scrap.getSourceType()) {
            case LINK -> firstNotBlank(scrap.getSourceUrl(), scrap.getRawContent());
            case TEXT -> scrap.getRawContent();
            case IMAGE -> resolveImageContent(scrap);
        };
    }

    /** 이미지 원본 AI 요청 content 선택 */
    private String resolveImageContent(Scrap scrap) {
        String imageObjectKey = scrap.getImageObjectKey();
        if (isBlank(imageObjectKey)) {
            return scrap.getRawContent();
        }

        return s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey);
    }

    /** 리콜 퀴즈 유형을 AI quiz_type으로 변환 */
    private String toAiQuizType(RecallQuizType quizType) {
        return switch (quizType) {
            case SHORT_ANSWER -> SHORT_ANSWER_AI_QUIZ_TYPE;
            case OX -> OX_AI_QUIZ_TYPE;
        };
    }

    /** OX 정답 문자열 변환 */
    private String toOxAnswer(Boolean isCorrect) {
        return Boolean.TRUE.equals(isCorrect) ? O_ANSWER : X_ANSWER;
    }

    /** 생성 결과 응답 변환 */
    private RecallQuizGenerateResponse toResponse(
            DailySummary summary,
            RecallQuizType quizType,
            List<RecallQuiz> quizzes
    ) {
        return new RecallQuizGenerateResponse(
                summary.getTargetDate(),
                quizType,
                quizzes.stream()
                        .map(RecallQuizResponse::from)
                        .toList()
        );
    }

    /** 첫 번째 유효 문자열 선택 */
    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /** 빈 문자열 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
