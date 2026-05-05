package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** 매일의 요약 조회 및 생성 결과 저장 Service */
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 사용자와 대상 날짜 기준 매일의 요약 조회 또는 생성
     *
     * @param userId 사용자 ID
     * @param targetDate 요약 대상 날짜
     * @return 기존 또는 새로 생성된 매일의 요약
     */
    @Transactional
    public DailySummary getOrCreateSummary(UUID userId, LocalDate targetDate) {
        Optional<DailySummary> summary = dailySummaryRepository.findByUser_UserIdAndTargetDate(userId, targetDate);
        if (summary.isPresent()) {
            return summary.get();
        }

        try {
            return createSummaryInNewTransaction(userId, targetDate);
        } catch (DataIntegrityViolationException e) {
            return getSummaryByUserAndTargetDate(userId, targetDate);
        }
    }

    /**
     * 매일의 요약 단건 조회
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
     * AI TIL 생성 결과 저장
     *
     * @param summaryId 매일의 요약 ID
     * @param content 생성된 TIL 마크다운 내용
     * @param embedding 생성된 TIL 임베딩
     */
    @Transactional
    public void updateGeneratedResult(UUID summaryId, String content, float[] embedding) {
        DailySummary summary = dailySummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));

        summary.updateGeneratedResult(content, embedding);
    }

    /**
     * 매일의 요약 엔티티 생성
     *
     * @param userId 사용자 ID
     * @param targetDate 요약 대상 날짜
     * @return 새 매일의 요약 엔티티
     */
    private DailySummary createSummary(UUID userId, LocalDate targetDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        return DailySummary.create(user, targetDate);
    }

    /**
     * 새 트랜잭션에서 매일의 요약 엔티티 생성 및 저장
     *
     * @param userId 사용자 ID
     * @param targetDate 요약 대상 날짜
     * @return 저장된 매일의 요약
     */
    private DailySummary createSummaryInNewTransaction(UUID userId, LocalDate targetDate) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate.execute(status -> dailySummaryRepository.saveAndFlush(createSummary(userId, targetDate)));
    }

    /**
     * 사용자와 대상 날짜 기준 매일의 요약 조회
     *
     * @param userId 사용자 ID
     * @param targetDate 요약 대상 날짜
     * @return 조회된 매일의 요약
     */
    private DailySummary getSummaryByUserAndTargetDate(UUID userId, LocalDate targetDate) {
        return dailySummaryRepository.findByUser_UserIdAndTargetDate(userId, targetDate)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
    }
}
