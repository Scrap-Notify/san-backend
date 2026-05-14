package com.san.api.domain.scrap.service;

import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScrapRefineJobProcessorTest {

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private ScrapRefineService scrapRefineService;

    @InjectMocks
    private ScrapRefineJobProcessor scrapRefineJobProcessor;

    @Test
    void process_refinesScrapAndCompletesJob() {
        UUID jobId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();

        scrapRefineJobProcessor.process(jobId, scrapId);

        verify(asyncJobManager).markProcessing(jobId);
        verify(scrapRefineService).refine(scrapId);
        verify(asyncJobManager).markCompleted(jobId);
    }

    @Test
    void process_whenRefineFails_marksJobFailed() {
        UUID jobId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("refine failed");
        doThrow(exception).when(scrapRefineService).refine(scrapId);

        scrapRefineJobProcessor.process(jobId, scrapId);

        verify(asyncJobManager).markProcessing(jobId);
        verify(scrapRefineService).refine(scrapId);
        verify(asyncJobManager).markFailed(jobId, "RuntimeException: refine failed");
    }
}
