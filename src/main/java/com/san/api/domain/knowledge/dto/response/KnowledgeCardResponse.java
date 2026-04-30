package com.san.api.domain.knowledge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 지식 카드 응답 DTO */
public record KnowledgeCardResponse(
        UUID cardId,
        String title,
        String summary,
        CategoryResponse category,
        List<TagResponse> tags,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
