package com.san.api.domain.github.service;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationJobProcessorTest {

    private AsyncJobManager asyncJobManager;
    private GithubStarRecommendationAnalysisService githubStarRecommendationAnalysisService;
    private GithubStarRecommendationJobProcessor processor;

    @BeforeEach
    void setUp() {
        asyncJobManager = mock(AsyncJobManager.class);
        githubStarRecommendationAnalysisService = mock(GithubStarRecommendationAnalysisService.class);
        processor = new GithubStarRecommendationJobProcessor(
                asyncJobManager,
                githubStarRecommendationAnalysisService
        );
    }

    @Test
    void handle_processesGithubStarRecommendationJobOnly() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        JobCreatedEvent event = mock(JobCreatedEvent.class);

        when(event.getJobType()).thenReturn(JobType.GITHUB_STAR_RECOMMENDATION);
        when(event.getJobId()).thenReturn(jobId);
        when(event.getTargetId()).thenReturn(targetId);

        processor.handle(event);

        verify(asyncJobManager).markProcessing(jobId);
        verify(githubStarRecommendationAnalysisService).analyzeAndSave(targetId, jobId);
        verify(asyncJobManager).markCompleted(jobId);
    }

    @Test
    void handle_ignoresOtherJobType() {
        UUID jobId = UUID.randomUUID();
        JobCreatedEvent event = mock(JobCreatedEvent.class);

        when(event.getJobType()).thenReturn(JobType.CARD_ANALYSIS);
        when(event.getJobId()).thenReturn(jobId);

        processor.handle(event);

        verify(asyncJobManager, never()).markProcessing(jobId);
        verify(githubStarRecommendationAnalysisService, never()).analyzeAndSave(null, jobId);
    }

    @Test
    void process_marksFailedWhenAnalysisFails() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("analysis failed");

        when(githubStarRecommendationAnalysisService.analyzeAndSave(targetId, jobId)).thenThrow(exception);

        processor.process(jobId, targetId);

        verify(asyncJobManager).markProcessing(jobId);
        verify(asyncJobManager).markFailed(jobId, "RuntimeException: analysis failed");
    }
}
