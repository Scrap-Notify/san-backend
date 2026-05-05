package com.san.api.domain.til.service;

import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** TIL 생성 작업 등록 Service */
@Service
@RequiredArgsConstructor
public class TilService {

    private final DailySummaryService dailySummaryService;
    private final AsyncJobManager asyncJobManager;

    /**
     * TIL 생성 비동기 작업 등록
     *
     * @param userId 로그인 사용자 ID
     * @param request TIL 생성 작업 등록 요청
     * @return 등록된 TIL 생성 작업 응답
     */
    @Transactional
    public TilGenerationJobResponse requestGeneration(UUID userId, TilGenerateRequest request) {
        DailySummary summary = dailySummaryService.getOrCreateSummary(userId, request.targetDate());
        UUID jobId = asyncJobManager.enqueue(JobType.TIL_GENERATION, summary.getSummaryId());

        return new TilGenerationJobResponse(
                summary.getSummaryId(),
                jobId,
                summary.getTargetDate()
        );
    }
}
