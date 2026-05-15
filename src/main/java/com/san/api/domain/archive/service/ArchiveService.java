package com.san.api.domain.archive.service;

import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryResponse;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 아카이브 조회 Service */
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final CategoryRepository categoryRepository;

    /**
     * 아카이브 카테고리 목록과 카테고리별 지식카드 개수 조회
     *
     * @param userId 로그인 사용자 ID
     * @return 아카이브 카테고리 목록 응답
     */
    @Transactional(readOnly = true)
    public ArchiveCategoryListResponse getCategories(UUID userId) {
        return new ArchiveCategoryListResponse(
                categoryRepository.findArchiveCategoryCounts(userId)
                        .stream()
                        .map(ArchiveCategoryResponse::from)
                        .toList()
        );
    }
}
