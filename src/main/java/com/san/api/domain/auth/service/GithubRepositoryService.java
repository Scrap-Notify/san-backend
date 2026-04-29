package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.auth.dto.response.GithubRepositoryResponse;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubRepository;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private final GithubApiClient githubApiClient;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final AesGcmStringEncryptor encryptor;

    /**
     * 로그인 사용자의 GitHub access token으로 접근 가능한 저장소 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<GithubRepositoryResponse> findRepositories(UUID userId) {
        return findGithubRepositories(userId).stream()
                .map(GithubRepositoryResponse::from)
                .toList();
    }

    /**
     * 사용자가 선택한 GitHub 저장소를 서비스 계정에 연결합니다.
     *
     * 클라이언트를 통해 받은 저장소 ID를 신뢰하지 않고(보안상) GitHub API 목록에서 다시 조회해,
     * 사용자가 실제 접근 가능한 저장소만 연결합니다.     */
    @Transactional
    public GithubRepositoryResponse connectRepository(UUID userId, GithubRepositoryConnectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        GithubRepository repository = findGithubRepositories(userId).stream()
                .filter(item -> item.id().equals(request.githubRepositoryId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND));

        GithubRepositoryConnection connection = connectionRepository
                .findByUser_UserIdAndGithubRepositoryId(userId, repository.id())
                .map(existing -> {
                    existing.update(repository);
                    return existing;
                })
                .orElseGet(() -> connectionRepository.save(new GithubRepositoryConnection(user, repository)));

        return GithubRepositoryResponse.from(connection);
    }

    /**
     * 서비스에 연결된 GitHub 저장소 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<GithubRepositoryResponse> findConnectedRepositories(UUID userId) {
        return connectionRepository.findAllByUser_UserId(userId).stream()
                .map(GithubRepositoryResponse::from)
                .toList();
    }

    /**
     * 서비스에 연결된 GitHub 저장소를 해제합니다.
     */
    @Transactional
    public void disconnectRepository(UUID userId, Long repositoryId) {
        GithubRepositoryConnection connection = connectionRepository
                .findByUser_UserIdAndGithubRepositoryId(userId, repositoryId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND));

        connectionRepository.delete(connection);
    }

    private List<GithubRepository> findGithubRepositories(UUID userId) {
        GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
        String accessToken = encryptor.decrypt(githubAccount.getAccessTokenEncrypted());
        return githubApiClient.findRepositories(accessToken);
    }
}
