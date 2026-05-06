package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** TIL GitHub 커밋 요청을 검증하고 비동기 작업으로 등록하는 서비스 */
@Service
@RequiredArgsConstructor
public class TilGithubCommitService {

    private static final List<TilGithubCommitStatus> DUPLICATE_CHECK_STATUSES = List.of(
            TilGithubCommitStatus.PENDING,
            TilGithubCommitStatus.PROCESSING,
            TilGithubCommitStatus.COMPLETED
    );

    private final DailySummaryService dailySummaryService;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository githubRepositoryConnectionRepository;
    private final TilGithubCommitRepository tilGithubCommitRepository;
    private final TilGithubFilePolicy filePolicy;
    private final TilGithubFilePathResolver filePathResolver;
    private final AsyncJobManager asyncJobManager;
    private final AesGcmStringEncryptor encryptor;

    /**
     * TIL GitHub 커밋 요청을 등록합니다.
     *
     * @param userId 요청 사용자 ID
     * @param summaryId 커밋할 TIL ID
     * @return 등록된 커밋 요청과 비동기 작업 ID
     */
    @Transactional
    public RequestResult requestCommit(UUID userId, UUID summaryId) {
        DailySummary summary = dailySummaryService.getSummary(summaryId);
        validateSummaryOwner(summary, userId);
        validateGeneratedTil(summary);

        GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
        GithubRepositoryConnection repositoryConnection = findSingleRepositoryConnection(userId);

        String branch = repositoryConnection.getDefaultBranch();
        String contentHash = filePolicy.createContentHash(summary.getContent());
        validateDuplicateContent(repositoryConnection, branch, contentHash);

        String accessToken = encryptor.decrypt(githubAccount.getAccessTokenEncrypted());
        String filePath = filePathResolver.resolve(
                accessToken,
                repositoryConnection.getFullName(),
                branch,
                summary.getTargetDate(),
                summary.getTitle()
        );
        String commitMessage = filePolicy.createCommitMessage(summary.getTitle());

        TilGithubCommit commit = tilGithubCommitRepository.save(TilGithubCommit.builder()
                .dailySummary(summary)
                .githubRepositoryConnection(repositoryConnection)
                .branch(branch)
                .filePath(filePath)
                .title(summary.getTitle().trim())
                .contentHash(contentHash)
                .commitMessage(commitMessage)
                .build());

        UUID jobId = asyncJobManager.enqueue(JobType.TIL_GITHUB_COMMIT, commit.getTilGithubCommitId());
        return new RequestResult(
                commit.getTilGithubCommitId(),
                jobId,
                summary.getSummaryId(),
                commit.getStatus()
        );
    }

    private void validateSummaryOwner(DailySummary summary, UUID userId) {
        if (!summary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(TilErrorCode.SUMMARY_ACCESS_DENIED);
        }
    }

    private void validateGeneratedTil(DailySummary summary) {
        if (isBlank(summary.getTitle())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "TIL 제목이 없어 GitHub 커밋을 요청할 수 없습니다.");
        }
        if (isBlank(summary.getContent())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "TIL 본문이 없어 GitHub 커밋을 요청할 수 없습니다.");
        }
    }

    private void validateDuplicateContent(
            GithubRepositoryConnection repositoryConnection,
            String branch,
            String contentHash
    ) {
        boolean duplicated = tilGithubCommitRepository
                .existsDuplicateContent(
                        repositoryConnection.getGithubRepositoryConnectionId(),
                        branch,
                        contentHash,
                        DUPLICATE_CHECK_STATUSES
                );
        if (duplicated) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE, "동일한 TIL 내용이 이미 GitHub 커밋 요청 또는 완료 상태입니다.");
        }
    }

    private GithubRepositoryConnection findSingleRepositoryConnection(UUID userId) {
        List<GithubRepositoryConnection> connections = githubRepositoryConnectionRepository.findAllByUser_UserId(userId);
        if (connections.isEmpty()) {
            throw new BusinessException(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND);
        }
        if (connections.size() > 1) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "GitHub 커밋 대상 저장소를 하나만 연결해주세요.");
        }
        return connections.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** TIL GitHub 커밋 요청 등록 결과 */
    public record RequestResult(
            UUID commitId,
            UUID jobId,
            UUID summaryId,
            TilGithubCommitStatus status
    ) {
    }
}
