package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.service.TokenIssueService;
import com.san.api.domain.github.dto.request.GithubLoginRequest;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.GithubUserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GitHub OAuth 로그인 callback과 연결 계정 로그인 처리를 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubAuthServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private TokenIssueService tokenIssueService;

    @Mock
    private GithubLinkService githubLinkService;

    @Mock
    private StringRedisTemplate redisTemplate;

    private GithubAuthService githubAuthService;

    @BeforeEach
    void setUp() {
        githubAuthService = new GithubAuthService(
                githubApiClient,
                userRepository,
                githubAccountRepository,
                tokenIssueService,
                githubLinkService,
                redisTemplate,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(githubAuthService, "successRedirectUri", "http://localhost/success");
        ReflectionTestUtils.setField(githubAuthService, "failureRedirectUri", "http://localhost/failure");
    }

    @Test
    void loginUsesUserAlreadyLinkedWithGithubAccount() {
        User user = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        GithubAccessTokenResponse githubToken = new GithubAccessTokenResponse("github-token", "bearer", "repo");
        GithubUserProfile profile = new GithubUserProfile(1L, "octocat");
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800);

        when(githubApiClient.requestAccessToken("code")).thenReturn(githubToken);
        when(githubApiClient.findUserProfile("github-token")).thenReturn(profile);
        when(githubAccountRepository.findByGithubUserId("1")).thenReturn(Optional.of(githubAccount));
        when(tokenIssueService.issueTokenPair(user.getUserId().toString())).thenReturn(tokenResponse);

        TokenResponse result = githubAuthService.login(new GithubLoginRequest("code"));

        assertThat(result).isEqualTo(tokenResponse);
        verify(githubLinkService).saveGithubAccount(user, profile, "github-token");
    }
}
