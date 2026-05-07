package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ScrapService 비즈니스 로직 단위 테스트 */
@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserRepository userRepository;

    private ScrapContentHashPolicy contentHashPolicy;
    private ScrapService scrapService;

    @BeforeEach
    void setUp() {
        contentHashPolicy = new ScrapContentHashPolicy();
        scrapService = new ScrapService(
                scrapRepository,
                userRepository,
                new SourceTypeDetector(),
                contentHashPolicy
        );
    }

    @Test
    void createScrap_savesNormalizedRawContentAndContentHash() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello\r\nworld \n");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.save(org.mockito.ArgumentMatchers.any(Scrap.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        scrapService.createScrap(userId, request);

        ArgumentCaptor<Scrap> captor = ArgumentCaptor.forClass(Scrap.class);
        verify(scrapRepository).save(captor.capture());
        Scrap saved = captor.getValue();

        assertThat(saved.getSourceType()).isEqualTo(SourceType.TEXT);
        assertThat(saved.getRawContent()).isEqualTo("hello\nworld");
        assertThat(saved.getContentHash()).isEqualTo(contentHashPolicy.createContentHash("hello\nworld"));
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
