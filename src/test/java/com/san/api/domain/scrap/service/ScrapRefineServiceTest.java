package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiScrapRefineClient;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapRefineServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private AiScrapRefineClient aiScrapRefineClient;

    @InjectMocks
    private ScrapRefineService scrapRefineService;

    @Test
    void refine_updatesRefinedContent() {
        UUID scrapId = UUID.randomUUID();
        Scrap scrap = buildScrap("raw content");

        when(scrapRepository.findById(scrapId)).thenReturn(Optional.of(scrap));
        when(aiScrapRefineClient.refine(org.mockito.ArgumentMatchers.any(AiScrapRefineRequest.class)))
                .thenReturn(new AiScrapRefineResponse("refined content"));

        scrapRefineService.refine(scrapId);

        ArgumentCaptor<AiScrapRefineRequest> captor = ArgumentCaptor.forClass(AiScrapRefineRequest.class);
        verify(aiScrapRefineClient).refine(captor.capture());
        assertThat(captor.getValue().inputType()).isEqualTo(SourceType.TEXT.name());
        assertThat(captor.getValue().content()).isEqualTo("raw content");
        assertThat(scrap.getRefinedContent()).isEqualTo("refined content");
    }

    @Test
    void refine_notFound_throwsException() {
        UUID scrapId = UUID.randomUUID();
        when(scrapRepository.findById(scrapId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapRefineService.refine(scrapId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(aiScrapRefineClient);
    }

    private Scrap buildScrap(String rawContent) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();

        return Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent(rawContent)
                .contentHash("hash")
                .build();
    }
}
