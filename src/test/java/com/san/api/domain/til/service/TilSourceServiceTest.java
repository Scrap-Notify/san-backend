package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.dto.response.TilGenerationSourceResponse;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilSourceServiceTest {

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private TilSourceService tilSourceService;

    @Test
    void getSource_convertsImageObjectKeyToPresignedUrl() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String imageObjectKey = "scrap/images/%s/image.png".formatted(userId);
        String presignedUrl = "https://bucket.s3.us-east-1.amazonaws.com/scrap/images/image.png?signature=test";
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.IMAGE)
                .rawContent("fallback")
                .imageObjectKey(imageObjectKey)
                .build();
        KnowledgeCard card = KnowledgeCard.builder()
                .scrap(scrap)
                .category(Category.builder().user(user).categoryName("Study").build())
                .title("Title")
                .summary("Summary")
                .build();

        when(knowledgeCardRepository.findTilSourceCards(
                eq(userId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(card));
        when(s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey)).thenReturn(presignedUrl);

        TilGenerationSourceResponse response = tilSourceService.getSource(userId, LocalDate.of(2026, 5, 12));

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).inputType()).isEqualTo("image");
        assertThat(response.contents().get(0).content()).isEqualTo(presignedUrl);
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
