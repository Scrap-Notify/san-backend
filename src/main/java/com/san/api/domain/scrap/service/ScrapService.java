package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** 수집 원본 저장 Service */
@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final SourceTypeDetector sourceTypeDetector;
    private final ScrapContentHashPolicy contentHashPolicy;
    private final AsyncJobManager asyncJobManager;
    private final AsyncJobRepository asyncJobRepository;

    /**
     * 수집 원본 저장
     *
     * @param userId 로그인 사용자 ID
     * @param request 수집 원본 저장 요청
     * @return 저장된 수집 원본 응답
     */
    public ScrapResponse createScrap(UUID userId, ScrapCreateRequest request) {
        validateRawContent(request.rawContent());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        String normalizedRawContent = contentHashPolicy.normalize(request.rawContent());
        String contentHash = contentHashPolicy.createContentHash(normalizedRawContent);
        SourceType sourceType = sourceTypeDetector.detect(normalizedRawContent);

        Optional<Scrap> existingScrap = scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(
                userId,
                sourceType,
                contentHash
        );
        if (existingScrap.isPresent()) {
            return createResponseWithJob(existingScrap.get());
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceUrl(blankToNull(request.sourceUrl()))
                .rawContent(normalizedRawContent)
                .contentHash(contentHash)
                .imageUrl(null)
                .build();

        Scrap savedScrap = scrapRepository.save(scrap);

        return createResponseWithJob(savedScrap);
    }

    /** 스크랩에 연결된 활성 분석 작업을 조회하거나 새로 등록 */
    private ScrapResponse createResponseWithJob(Scrap scrap) {
        UUID jobId = findActiveCardAnalysisJobId(scrap.getScrapId())
                .orElseGet(() -> asyncJobManager.enqueue(JobType.CARD_ANALYSIS, scrap.getScrapId()));

        return ScrapResponse.from(scrap, jobId);
    }

    /** 진행 중인 지식카드 분석 작업 ID 조회 */
    private Optional<UUID> findActiveCardAnalysisJobId(UUID scrapId) {
        return asyncJobRepository.findByTargetIdAndJobType(scrapId, JobType.CARD_ANALYSIS)
                .stream()
                .filter(this::isActiveJob)
                .map(AsyncJob::getJobId)
                .findFirst();
    }

    /** 활성 작업 여부 */
    private boolean isActiveJob(AsyncJob job) {
        return job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING;
    }

    /**
     * 원본 입력값 검증
     *
     * @param rawContent 사용자가 입력하거나 붙여넣은 원본 값
     */
    private void validateRawContent(String rawContent) {
        if (isBlank(rawContent)) {
            throw new BusinessException(ScrapErrorCode.EMPTY_SOURCE);
        }
    }

    /**
     * 빈 문자열 null 변환
     *
     * @param value 변환 대상 값
     * @return 공백 제거 값 또는 null
     */
    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 빈 값 여부
     *
     * @param value 검사할 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
