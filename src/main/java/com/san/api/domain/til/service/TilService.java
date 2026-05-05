package com.san.api.domain.til.service;

import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.dto.response.TilResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.TilErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** TIL 생성 작업 등록 및 조회 Service */
@Service
@RequiredArgsConstructor
public class TilService {

    private final DailySummaryRepository dailySummaryRepository;
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

    /**
     * 날짜 기준 TIL 조회
     *
     * @param userId 로그인 사용자 ID
     * @param targetDate 조회 대상 날짜
     * @return 날짜 기준 TIL 조회 응답
     */
    @Transactional(readOnly = true)
    public TilResponse getTil(UUID userId, LocalDate targetDate) {
        DailySummary summary = dailySummaryRepository.findByUser_UserIdAndTargetDate(userId, targetDate)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));

        return TilResponse.from(summary);
    }
}
