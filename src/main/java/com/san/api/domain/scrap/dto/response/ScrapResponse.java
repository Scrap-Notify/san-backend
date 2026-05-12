package com.san.api.domain.scrap.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/** 스크랩 저장 및 조회 응답 DTO */
public record ScrapResponse(

        UUID scrapId,
        UUID jobId,
        UUID cardId,
        SourceType sourceType,
        String sourceUrl,
        String rawContent,
        String imageObjectKey,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ScrapResponse from(Scrap scrap, UUID jobId, UUID cardId) {
        return new ScrapResponse(
                scrap.getScrapId(),
                jobId,
                cardId,
                scrap.getSourceType(),
                scrap.getSourceUrl(),
                scrap.getRawContent(),
                scrap.getImageObjectKey(),
                scrap.getCreatedAt()
        );
    }
}
