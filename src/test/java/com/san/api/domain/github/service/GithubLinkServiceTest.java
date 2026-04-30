package com.san.api.domain.github.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubUserProfile;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GitHub 계정 연동 state 관리와 계정 연결 저장을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubLinkServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AesGcmStringEncryptor encryptor;

    private GithubLinkService githubLinkService;

    @BeforeEach
    void setUp() {
        githubLinkService = new GithubLinkService(
                githubApiClient,
                userRepository,
                githubAccountRepository,
                redisTemplate,
                encryptor
        );
    }

    @Test
    void createLinkAuthorizationRedirectUrlStoresUserIdByState() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(githubApiClient.createAuthorizationUrl(anyString())).thenReturn("https://github.com/oauth");

        String redirectUrl = githubLinkService.createLinkAuthorizationRedirectUrl(userId);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), any(Duration.class));
        assertThat(keyCaptor.getValue()).startsWith(AuthRedisKeyPrefix.GITHUB_LINK_STATE);
        assertThat(valueCaptor.getValue()).isEqualTo(userId.toString());
        assertThat(redirectUrl).isEqualTo("https://github.com/oauth");
    }

    @Test
    void consumeLinkUserIdReturnsUserIdAndDeletesState() {
        UUID userId = UUID.randomUUID();
        String state = "state-token";
        String key = AuthRedisKeyPrefix.GITHUB_LINK_STATE + state;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(userId.toString());

        Optional<UUID> result = githubLinkService.consumeLinkUserId(state);

        assertThat(result).contains(userId);
        verify(redisTemplate).delete(key);
    }

    @Test
    void saveGithubAccountRejectsAlreadyLinkedGithubAccount() {
        User currentUser = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        User otherUser = User.builder()
                .username("otheruser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        GithubUserProfile profile = new GithubUserProfile(1L, "octocat");
        GithubAccount linkedAccount = new GithubAccount(otherUser, "1", "octocat", "encrypted-token");
        when(encryptor.encrypt("github-token")).thenReturn("new-encrypted-token");
        when(githubAccountRepository.findByGithubUserId("1")).thenReturn(Optional.of(linkedAccount));

        assertThatThrownBy(() -> githubLinkService.saveGithubAccount(currentUser, profile, "github-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED);
    }
}
