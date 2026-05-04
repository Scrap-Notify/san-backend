package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.dto.response.TilSourceContentResponse;
import com.san.api.domain.til.dto.response.TilSourceResponse;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** TIL 생성용 지식 원본 구성 Service */
@Service
@RequiredArgsConstructor
public class TilSourceService {

    private final KnowledgeCardRepository knowledgeCardRepository;

    /**
     * TIL 생성에 사용할 지식카드 원본 목록 구성
     *
     * @param userId 사용자 ID
     * @param targetDate TIL 생성 대상 날짜
     * @return TIL 생성용 지식 원본 목록
     */
    @Transactional(readOnly = true)
    public TilSourceResponse getSource(UUID userId, LocalDate targetDate) {
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        return new TilSourceResponse(
                targetDate,
                knowledgeCardRepository.findTilSourceCards(userId, startAt, endAt).stream()
                        .map(KnowledgeCard::getScrap)
                        .map(this::toSourceContent)
                        .toList()
        );
    }

    /** 지식 원본을 AI TIL 생성 요청 형식으로 변환 */
    private TilSourceContentResponse toSourceContent(Scrap scrap) {
        String content = resolveContent(scrap);
        if (isBlank(content)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        return new TilSourceContentResponse(toInputType(scrap.getSourceType()), content.trim());
    }

    /** SourceType을 AI input_type 값으로 변환 */
    private String toInputType(SourceType sourceType) {
        return switch (sourceType) {
            case LINK -> "url";
            case TEXT -> "text";
            case IMAGE -> "image";
        };
    }

    /** 원본 유형별 AI 전달 content 선택 */
    private String resolveContent(Scrap scrap) {
        return switch (scrap.getSourceType()) {
            case LINK -> firstNotBlank(scrap.getSourceUrl(), scrap.getRawContent());
            case TEXT -> scrap.getRawContent();
            case IMAGE -> firstNotBlank(scrap.getImageUrl(), scrap.getRawContent());
        };
    }

    /** 비어있지 않은 첫 번째 문자열 반환 */
    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /** 빈 문자열 여부 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
