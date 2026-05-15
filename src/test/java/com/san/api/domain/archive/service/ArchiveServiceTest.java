package com.san.api.domain.archive.service;

import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    void getCategories_returnsCategoryCounts() {
        UUID userId = UUID.randomUUID();
        UUID backendCategoryId = UUID.randomUUID();
        UUID securityCategoryId = UUID.randomUUID();

        when(categoryRepository.findArchiveCategoryCounts(userId))
                .thenReturn(List.of(
                        new TestCategoryCardCountProjection(backendCategoryId, "백엔드", 3),
                        new TestCategoryCardCountProjection(securityCategoryId, "보안", 1)
                ));

        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories()).extracting("categoryId")
                .containsExactly(backendCategoryId, securityCategoryId);
        assertThat(response.categories()).extracting("categoryName")
                .containsExactly("백엔드", "보안");
        assertThat(response.categories()).extracting("cardCount")
                .containsExactly(3L, 1L);
    }

    @Test
    void getCategories_returnsEmptyListWhenNoArchivedCardsExist() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findArchiveCategoryCounts(userId)).thenReturn(List.of());

        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        assertThat(response.categories()).isEmpty();
    }

    private record TestCategoryCardCountProjection(
            UUID categoryId,
            String categoryName,
            long cardCount
    ) implements CategoryRepository.CategoryCardCountProjection {

        @Override
        public UUID getCategoryId() {
            return categoryId;
        }

        @Override
        public String getCategoryName() {
            return categoryName;
        }

        @Override
        public long getCardCount() {
            return cardCount;
        }
    }
}
