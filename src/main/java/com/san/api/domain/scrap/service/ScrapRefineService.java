package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiScrapRefineClient;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 수집 원본 정제 Service */
@Service
@RequiredArgsConstructor
public class ScrapRefineService {

    private final ScrapRepository scrapRepository;
    private final AiScrapRefineClient aiScrapRefineClient;

    /**
     * 수집 원본을 AI 서버로 정제하고 저장
     *
     * @param scrapId 정제 대상 Scrap ID
     */
    @Transactional
    public void refine(UUID scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        AiScrapRefineResponse response = aiScrapRefineClient.refine(new AiScrapRefineRequest(
                scrap.getSourceType().name(),
                scrap.getRawContent()
        ));
        scrap.updateRefinedContent(response.refinedContent());
    }
}
