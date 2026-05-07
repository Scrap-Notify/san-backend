package com.san.api.domain.scrap.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/** 스크랩 조회 응답 DTO */
public record ScrapResponse(

        UUID scrapId,
        UUID jobId,
        SourceType sourceType,
        String sourceUrl,
        String rawContent,
        String imageUrl,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ScrapResponse from(Scrap scrap) {
        return from(scrap, null);
    }

    public static ScrapResponse from(Scrap scrap, UUID jobId) {
        return new ScrapResponse(
                scrap.getScrapId(),
                jobId,
                scrap.getSourceType(),
                scrap.getSourceUrl(),
                scrap.getRawContent(),
                scrap.getImageUrl(),
                scrap.getCreatedAt()
        );
    }
}
