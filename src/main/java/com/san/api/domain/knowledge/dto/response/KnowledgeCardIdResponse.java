package com.san.api.domain.knowledge.dto.response;

import java.util.UUID;

public record KnowledgeCardIdResponse(
        UUID scrapId,
        UUID cardId
) {
    public static KnowledgeCardIdResponse from(UUID scrapId, UUID cardId) {
        return new KnowledgeCardIdResponse(scrapId, cardId);
    }
}
