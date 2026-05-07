package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ScrapService 비즈니스 로직 단위 테스트 */
@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private AsyncJobRepository asyncJobRepository;

    private ScrapContentHashPolicy contentHashPolicy;
    private ScrapService scrapService;

    @BeforeEach
    void setUp() {
        contentHashPolicy = new ScrapContentHashPolicy();
        scrapService = new ScrapService(
                scrapRepository,
                userRepository,
                new SourceTypeDetector(),
                contentHashPolicy,
                asyncJobManager,
                asyncJobRepository
        );
    }

    @Test
    void createScrap_savesNormalizedRawContentAndContentHash() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello\r\nworld \n");
        String contentHash = contentHashPolicy.createContentHash("hello\nworld");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UUID jobId = UUID.randomUUID();
        when(asyncJobRepository.findByTargetIdAndJobType(any(UUID.class), eq(JobType.CARD_ANALYSIS)))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(eq(JobType.CARD_ANALYSIS), any(UUID.class))).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        ArgumentCaptor<Scrap> captor = ArgumentCaptor.forClass(Scrap.class);
        verify(scrapRepository).save(captor.capture());
        Scrap saved = captor.getValue();

        assertThat(saved.getSourceType()).isEqualTo(SourceType.TEXT);
        assertThat(saved.getRawContent()).isEqualTo("hello\nworld");
        assertThat(saved.getContentHash()).isEqualTo(contentHash);
        assertThat(response.jobId()).isEqualTo(jobId);
    }

    @Test
    void createScrap_returnsExistingScrapWithNewJobWhenSameSourceExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        UUID jobId = UUID.randomUUID();
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId())).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(scrapRepository, never()).save(any(Scrap.class));
        assertThat(response.scrapId()).isEqualTo(existingScrap.getScrapId());
        assertThat(response.jobId()).isEqualTo(jobId);
    }

    @Test
    void createScrap_returnsActiveJobWhenSameSourceJobExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID jobId = UUID.randomUUID();
        AsyncJob activeJob = buildJob(jobId, JobType.CARD_ANALYSIS, JobStatus.PROCESSING, existingScrap.getScrapId());
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of(activeJob));

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(asyncJobManager, never()).enqueue(any(JobType.class), any(UUID.class));
        assertThat(response.jobId()).isEqualTo(jobId);
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private AsyncJob buildJob(UUID jobId, JobType jobType, JobStatus status, UUID targetId) {
        AsyncJob job = AsyncJob.builder()
                .jobType(jobType)
                .targetId(targetId)
                .build();
        ReflectionTestUtils.setField(job, "jobId", jobId);
        job.updateStatus(status);
        return job;
    }
}
