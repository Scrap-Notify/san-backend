package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.auth.dto.response.GithubRepositoryResponse;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubRepository;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GitHub 레포지토리 조회와 연결 서비스 동작을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubRepositoryServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private GithubRepositoryConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AesGcmStringEncryptor encryptor;

    private GithubRepositoryService githubRepositoryService;
    private User user;
    private UUID userId;
    private GithubAccount githubAccount;
    private GithubRepository repository;

    @BeforeEach
    void setUp() {
        githubRepositoryService = new GithubRepositoryService(
                githubApiClient,
                githubAccountRepository,
                connectionRepository,
                userRepository,
                encryptor
        );
        user = User.builder()
                .username("gh_123")
                .provider(AuthProvider.GITHUB)
                .providerId("123")
                .build();
        userId = user.getUserId();
        githubAccount = new GithubAccount(user, "123", "octocat", "encrypted-token");
        repository = new GithubRepository(
                100L,
                "algorithm",
                "octocat/algorithm",
                false,
                "main",
                "https://github.com/octocat/algorithm"
        );
    }

    @Test
    void findRepositoriesUsesStoredGithubToken() {
        mockGithubRepositoryLookup();

        List<GithubRepositoryResponse> responses = githubRepositoryService.findRepositories(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).githubRepositoryId()).isEqualTo(100L);
        assertThat(responses.get(0).fullName()).isEqualTo("octocat/algorithm");
        verify(encryptor).decrypt("encrypted-token");
        verify(githubApiClient).findRepositories("plain-token");
    }

    @Test
    void connectRepositoryStoresOnlyRepositoryAccessibleFromGithub() {
        mockGithubRepositoryLookup();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(connectionRepository.findByUser_UserIdAndGithubRepositoryId(userId, 100L))
                .thenReturn(Optional.empty());
        when(connectionRepository.save(any(GithubRepositoryConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GithubRepositoryResponse response = githubRepositoryService.connectRepository(
                userId,
                new GithubRepositoryConnectRequest(100L)
        );

        assertThat(response.githubRepositoryId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("algorithm");
        assertThat(response.defaultBranch()).isEqualTo("main");
        verify(connectionRepository).save(any(GithubRepositoryConnection.class));
    }

    @Test
    void connectRepositoryRejectsRepositoryNotReturnedByGithub() {
        mockGithubRepositoryLookup();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> githubRepositoryService.connectRepository(
                userId,
                new GithubRepositoryConnectRequest(999L)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND);
    }

    private void mockGithubRepositoryLookup() {
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(encryptor.decrypt("encrypted-token")).thenReturn("plain-token");
        when(githubApiClient.findRepositories("plain-token")).thenReturn(List.of(repository));
    }
}
