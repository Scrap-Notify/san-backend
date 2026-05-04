package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 수집 원본 저장 Service */
@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final SourceTypeDetector sourceTypeDetector;

    /**
     * 수집 원본 저장
     *
     * @param userId 로그인 사용자 ID
     * @param request 수집 원본 저장 요청
     * @return 저장된 수집 원본 응답
     */
    @Transactional
    public ScrapResponse createScrap(UUID userId, ScrapCreateRequest request) {
        validateRawContent(request.rawContent());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(sourceTypeDetector.detect(request.rawContent()))
                .sourceUrl(blankToNull(request.sourceUrl()))
                .rawContent(request.rawContent().trim())
                .imageUrl(null)
                .build();

        return ScrapResponse.from(scrapRepository.save(scrap));
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
