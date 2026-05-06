package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** 매일의 요약 조회 및 생성 결과 저장 Service */
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;
    private final UserRepository userRepository;

    /**
     * TIL 생성 요청마다 새로운 빈 매일의 요약을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param targetDate TIL 대상 날짜
     * @return 새로 생성된 매일의 요약
     */
    @Transactional
    public DailySummary createSummary(UUID userId, LocalDate targetDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        return dailySummaryRepository.save(DailySummary.create(user, targetDate));
    }

    /**
     * 매일의 요약을 조회합니다.
     *
     * @param summaryId 매일의 요약 ID
     * @return 조회된 매일의 요약
     */
    @Transactional(readOnly = true)
    public DailySummary getSummary(UUID summaryId) {
        return dailySummaryRepository.findBySummaryIdWithUser(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
    }

    /**
     * 쓰기 잠금을 적용해 매일의 요약을 조회합니다.
     *
     * @param summaryId 매일의 요약 ID
     * @return 쓰기 잠금이 적용된 매일의 요약
     */
    @Transactional
    public DailySummary getSummaryForUpdate(UUID summaryId) {
        return dailySummaryRepository.findBySummaryIdWithUserForUpdate(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
    }

    /**
     * AI가 생성한 TIL 결과를 저장합니다.
     *
     * @param summaryId 매일의 요약 ID
     * @param title 생성된 TIL 제목
     * @param content 생성된 TIL 마크다운 본문
     * @param embedding 생성된 TIL 임베딩
     */
    @Transactional
    public void updateGeneratedResult(UUID summaryId, String title, String content, float[] embedding) {
        DailySummary summary = dailySummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));

        summary.updateGeneratedResult(title, content, embedding);
    }
}
