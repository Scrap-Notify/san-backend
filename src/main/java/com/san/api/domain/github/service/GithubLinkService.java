package com.san.api.domain.github.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.GithubUserProfile;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** 로그인된 서비스 사용자와 GitHub 계정의 연동 흐름을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
public class GithubLinkService {

    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final GithubApiClient githubApiClient;
    private final UserRepository userRepository;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository connectionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AesGcmStringEncryptor encryptor;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createLinkAuthorizationRedirectUrl(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        String state = generateUrlSafeToken();
        redisTemplate.opsForValue().set(
                AuthRedisKeyPrefix.GITHUB_LINK_STATE + state,
                userId.toString(),
                STATE_TTL
        );
        return githubApiClient.createAuthorizationUrl(state);
    }

    public Optional<UUID> consumeLinkUserId(String state) {
        String key = AuthRedisKeyPrefix.GITHUB_LINK_STATE + state;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return Optional.empty();
        }

        redisTemplate.delete(key);
        return Optional.of(UUID.fromString(userId));
    }

    @Transactional
    public void linkGithubAccount(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        GithubAccessTokenResponse tokenResponse = githubApiClient.requestAccessToken(code);
        GithubUserProfile profile = githubApiClient.findUserProfile(tokenResponse.accessToken());

        saveGithubAccount(user, profile, tokenResponse.accessToken());
    }

    @Transactional
    public void saveGithubAccount(User user, GithubUserProfile profile, String accessToken) {
        String githubUserId = profile.id().toString();
        String encryptedToken = encryptor.encrypt(accessToken);

        githubAccountRepository.findByGithubUserId(githubUserId)
                .ifPresentOrElse(
                        account -> updateOwnAccount(account, user, profile.login(), encryptedToken),
                        () -> githubAccountRepository.save(
                                new GithubAccount(user, githubUserId, profile.login(), encryptedToken)
                        )
                );
    }

    @Transactional
    public void unlinkGithubAccount(UUID userId) {
        if (githubAccountRepository.findByUser_UserId(userId).isEmpty()) {
            throw new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED);
        }

        connectionRepository.deleteAllByUser_UserId(userId);
        githubAccountRepository.deleteByUser_UserId(userId);
    }

    private void updateOwnAccount(GithubAccount account, User user, String githubUsername, String encryptedToken) {
        if (!account.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED);
        }
        account.updateToken(githubUsername, encryptedToken);
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
