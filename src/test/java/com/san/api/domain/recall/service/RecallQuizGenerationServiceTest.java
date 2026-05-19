package com.san.api.domain.recall.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.dto.response.RecallQuizGenerateResponse;
import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizRepository;
import com.san.api.domain.recall.service.RecallQuizSourceService.RecallQuizSourceResult;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import com.san.api.global.external.ai.client.AiQuizClient;
import com.san.api.global.external.ai.dto.request.AiQuizRequest;
import com.san.api.global.external.ai.dto.response.AiOxQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiOxQuizResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecallQuizGenerationServiceTest {

    @Mock
    private RecallQuizSourceService recallQuizSourceService;
    @Mock
    private RecallQuizRepository recallQuizRepository;
    @Mock
    private RecallQuizPersistenceService recallQuizPersistenceService;
    @Mock
    private AiQuizClient aiQuizClient;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private RecallQuizGenerationService recallQuizGenerationService;

    private UUID userId;
    private User user;
    private LocalDate targetDate;
    private DailySummary summary;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = buildUser(userId);
        targetDate = LocalDate.of(2026, 5, 19);
        summary = buildSummary();
    }

    @Test
    void generateReturnsExistingQuizzesWithoutCallingAi() {
        RecallQuiz existingQuiz = RecallQuiz.builder()
                .dailySummary(summary)
                .scrap(buildScrap(SourceType.TEXT, null, "raw content", null))
                .quizType(RecallQuizType.SHORT_ANSWER)
                .question("기존 질문")
                .answer("기존 정답")
                .explanation("기존 해설")
                .build();

        when(recallQuizSourceService.findSources(userId, targetDate))
                .thenReturn(new RecallQuizSourceResult(summary, List.of(buildCard(SourceType.TEXT, null, "raw content", null))));
        when(recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                RecallQuizType.SHORT_ANSWER
        )).thenReturn(List.of(existingQuiz));

        RecallQuizGenerateResponse response = recallQuizGenerationService.generate(
                userId,
                new RecallQuizGenerateRequest(targetDate, RecallQuizType.SHORT_ANSWER)
        );

        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.quizType()).isEqualTo(RecallQuizType.SHORT_ANSWER);
        assertThat(response.quizzes()).hasSize(1);
        assertThat(response.quizzes().getFirst().question()).isEqualTo("기존 질문");
        verifyNoInteractions(aiQuizClient);
    }

    @Test
    void generateShortAnswerQuizCallsAiAndSavesQuizzes() {
        KnowledgeCard card = buildCard(SourceType.LINK, "https://example.com/article", "fallback", null);
        AiShortAnswerQuizResponse aiResponse = new AiShortAnswerQuizResponse(
                "short_answer",
                List.of(new AiShortAnswerQuizQuestionResponse("질문", "정답", "해설"))
        );
        ArgumentCaptor<AiQuizRequest> requestCaptor = ArgumentCaptor.forClass(AiQuizRequest.class);
        AtomicReference<List<RecallQuiz>> savedQuizzes = new AtomicReference<>();

        when(recallQuizSourceService.findSources(userId, targetDate))
                .thenReturn(new RecallQuizSourceResult(summary, List.of(card)));
        when(recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                RecallQuizType.SHORT_ANSWER
        )).thenReturn(List.of());
        when(aiQuizClient.generateShortAnswerQuiz(any(AiQuizRequest.class))).thenReturn(aiResponse);
        when(recallQuizPersistenceService.saveQuizzes(anyList())).thenAnswer(invocation -> {
            List<RecallQuiz> quizzes = invocation.getArgument(0);
            savedQuizzes.set(quizzes);
            return quizzes;
        });

        RecallQuizGenerateResponse response = recallQuizGenerationService.generate(
                userId,
                new RecallQuizGenerateRequest(targetDate, RecallQuizType.SHORT_ANSWER)
        );

        verify(aiQuizClient).generateShortAnswerQuiz(requestCaptor.capture());
        assertThat(requestCaptor.getValue().quizType()).isEqualTo("short_answer");
        assertThat(requestCaptor.getValue().contents()).hasSize(1);
        assertThat(requestCaptor.getValue().contents().getFirst().inputType()).isEqualTo("url");
        assertThat(requestCaptor.getValue().contents().getFirst().content()).isEqualTo("https://example.com/article");

        RecallQuiz savedQuiz = savedQuizzes.get().getFirst();
        assertThat(savedQuiz.getQuizType()).isEqualTo(RecallQuizType.SHORT_ANSWER);
        assertThat(savedQuiz.getQuestion()).isEqualTo("질문");
        assertThat(savedQuiz.getAnswer()).isEqualTo("정답");
        assertThat(savedQuiz.getExplanation()).isEqualTo("해설");
        assertThat(response.quizzes().getFirst().explanation()).isNull();
    }

    @Test
    void generateOxQuizCallsAiAndSavesOxAnswer() {
        KnowledgeCard card = buildCard(SourceType.TEXT, null, "React.memo는 모든 컴포넌트에 권장된다.", null);
        AiOxQuizResponse aiResponse = new AiOxQuizResponse(
                "ox",
                List.of(new AiOxQuizQuestionResponse("React.memo는 모든 컴포넌트에 권장된다.", false, "필요한 곳에만 사용한다."))
        );
        ArgumentCaptor<AiQuizRequest> requestCaptor = ArgumentCaptor.forClass(AiQuizRequest.class);
        AtomicReference<List<RecallQuiz>> savedQuizzes = new AtomicReference<>();

        when(recallQuizSourceService.findSources(userId, targetDate))
                .thenReturn(new RecallQuizSourceResult(summary, List.of(card)));
        when(recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                RecallQuizType.OX
        )).thenReturn(List.of());
        when(aiQuizClient.generateOxQuiz(any(AiQuizRequest.class))).thenReturn(aiResponse);
        when(recallQuizPersistenceService.saveQuizzes(anyList())).thenAnswer(invocation -> {
            List<RecallQuiz> quizzes = invocation.getArgument(0);
            savedQuizzes.set(quizzes);
            return quizzes;
        });

        RecallQuizGenerateResponse response = recallQuizGenerationService.generate(
                userId,
                new RecallQuizGenerateRequest(targetDate, RecallQuizType.OX)
        );

        verify(aiQuizClient).generateOxQuiz(requestCaptor.capture());
        assertThat(requestCaptor.getValue().quizType()).isEqualTo("ox");
        assertThat(requestCaptor.getValue().contents().getFirst().inputType()).isEqualTo("text");
        assertThat(requestCaptor.getValue().contents().getFirst().content()).isEqualTo("React.memo는 모든 컴포넌트에 권장된다.");

        RecallQuiz savedQuiz = savedQuizzes.get().getFirst();
        assertThat(savedQuiz.getQuizType()).isEqualTo(RecallQuizType.OX);
        assertThat(savedQuiz.getQuestion()).isEqualTo("React.memo는 모든 컴포넌트에 권장된다.");
        assertThat(savedQuiz.getAnswer()).isEqualTo("X");
        assertThat(savedQuiz.getExplanation()).isEqualTo("필요한 곳에만 사용한다.");
        assertThat(response.quizzes().getFirst().question()).isEqualTo("React.memo는 모든 컴포넌트에 권장된다.");
    }

    @Test
    void generateReturnsExistingQuizzesWhenSaveConflicts() {
        KnowledgeCard card = buildCard(SourceType.TEXT, null, "raw content", null);
        RecallQuiz conflictQuiz = RecallQuiz.builder()
                .dailySummary(summary)
                .scrap(card.getScrap())
                .quizType(RecallQuizType.SHORT_ANSWER)
                .question("conflict question")
                .answer("conflict answer")
                .explanation("conflict explanation")
                .build();
        AiShortAnswerQuizResponse aiResponse = new AiShortAnswerQuizResponse(
                "short_answer",
                List.of(new AiShortAnswerQuizQuestionResponse("question", "answer", "explanation"))
        );

        when(recallQuizSourceService.findSources(userId, targetDate))
                .thenReturn(new RecallQuizSourceResult(summary, List.of(card)));
        when(recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                RecallQuizType.SHORT_ANSWER
        )).thenReturn(List.of(), List.of(conflictQuiz));
        when(aiQuizClient.generateShortAnswerQuiz(any(AiQuizRequest.class))).thenReturn(aiResponse);
        when(recallQuizPersistenceService.saveQuizzes(anyList()))
                .thenThrow(new DataIntegrityViolationException("duplicate recall quiz"));

        RecallQuizGenerateResponse response = recallQuizGenerationService.generate(
                userId,
                new RecallQuizGenerateRequest(targetDate, RecallQuizType.SHORT_ANSWER)
        );

        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.quizType()).isEqualTo(RecallQuizType.SHORT_ANSWER);
        assertThat(response.quizzes()).hasSize(1);
        assertThat(response.quizzes().getFirst().question()).isEqualTo("conflict question");
        verify(recallQuizPersistenceService).saveQuizzes(anyList());
    }

    @Test
    void generateThrowsExceptionWhenSourceContentIsBlank() {
        KnowledgeCard card = buildCard(SourceType.TEXT, null, "   ", null);

        when(recallQuizSourceService.findSources(userId, targetDate))
                .thenReturn(new RecallQuizSourceResult(summary, List.of(card)));
        when(recallQuizRepository.findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
                userId,
                summary.getSummaryId(),
                RecallQuizType.SHORT_ANSWER
        )).thenReturn(List.of());

        assertThatThrownBy(() -> recallQuizGenerationService.generate(
                userId,
                new RecallQuizGenerateRequest(targetDate, RecallQuizType.SHORT_ANSWER)
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecallErrorCode.INVALID_RECALL_SOURCE_CONTENT);

        verifyNoInteractions(aiQuizClient);
    }

    private DailySummary buildSummary() {
        return DailySummary.builder()
                .user(user)
                .targetDate(targetDate)
                .title("TIL title")
                .content("TIL content")
                .build();
    }

    private KnowledgeCard buildCard(SourceType sourceType, String sourceUrl, String rawContent, String imageObjectKey) {
        return KnowledgeCard.builder()
                .scrap(buildScrap(sourceType, sourceUrl, rawContent, imageObjectKey))
                .category(Category.builder().user(user).categoryName("Backend").build())
                .title("Card title")
                .summary("Card summary")
                .build();
    }

    private Scrap buildScrap(SourceType sourceType, String sourceUrl, String rawContent, String imageObjectKey) {
        return Scrap.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceUrl(sourceUrl)
                .rawContent(rawContent)
                .imageObjectKey(imageObjectKey)
                .build();
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
