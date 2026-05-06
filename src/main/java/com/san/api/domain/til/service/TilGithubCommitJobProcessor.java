package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.GithubCreateContentResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/** TIL GitHub 커밋 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class TilGithubCommitJobProcessor implements AsyncJobProcessor {

    private final AsyncJobManager asyncJobManager;
    private final TilGithubCommitRepository tilGithubCommitRepository;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubApiClient githubApiClient;
    private final AesGcmStringEncryptor encryptor;
    private final PlatformTransactionManager transactionManager;

    /**
     * TIL_GITHUB_COMMIT 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("asyncJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        if (event.getJobType() != JobType.TIL_GITHUB_COMMIT) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

    /**
     * TIL GitHub 커밋 작업을 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 커밋 요청 ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        asyncJobManager.markProcessing(jobId);
        markCommitProcessing(targetId);

        try {
            CommitPayload payload = loadPayload(targetId);
            GithubCreateContentResponse response = githubApiClient.createContent(
                    payload.accessToken(),
                    payload.owner(),
                    payload.repo(),
                    payload.filePath(),
                    payload.branch(),
                    payload.commitMessage(),
                    encodeContent(payload.content())
            );
            markCommitCompleted(targetId, response.commit().sha(), response.commit().htmlUrl(), LocalDateTime.now());
            asyncJobManager.markCompleted(jobId);
        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            markCommitFailed(targetId, errorMessage);
            asyncJobManager.markFailed(jobId, errorMessage);
        }
    }

    private CommitPayload loadPayload(UUID commitId) {
        return transactionTemplate().execute(status -> {
            TilGithubCommit commit = getCommit(commitId);
            DailySummary summary = commit.getDailySummary();
            GithubRepositoryConnection repositoryConnection = commit.getGithubRepositoryConnection();
            GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(summary.getUser().getUserId())
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
            RepositoryName repositoryName = RepositoryName.from(repositoryConnection.getFullName());

            return new CommitPayload(
                    encryptor.decrypt(githubAccount.getAccessTokenEncrypted()),
                    repositoryName.owner(),
                    repositoryName.repo(),
                    commit.getFilePath(),
                    commit.getBranch(),
                    commit.getCommitMessage(),
                    summary.getContent()
            );
        });
    }

    private void markCommitProcessing(UUID commitId) {
        transactionTemplate().executeWithoutResult(status -> getCommit(commitId).markProcessing());
    }

    private void markCommitCompleted(UUID commitId, String commitSha, String commitUrl, LocalDateTime pushedAt) {
        transactionTemplate().executeWithoutResult(status -> {
            TilGithubCommit commit = getCommit(commitId);
            commit.markCompleted(commitSha, commitUrl, pushedAt);
        });
    }

    private void markCommitFailed(UUID commitId, String errorMessage) {
        transactionTemplate().executeWithoutResult(status -> getCommit(commitId).markFailed(errorMessage));
    }

    private TilGithubCommit getCommit(UUID commitId) {
        return tilGithubCommitRepository.findByIdWithSummaryAndRepository(commitId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
    }

    private String encodeContent(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "TIL GitHub 커밋 작업 처리 중 오류가 발생했습니다.";
        }

        return message;
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private record CommitPayload(
            String accessToken,
            String owner,
            String repo,
            String filePath,
            String branch,
            String commitMessage,
            String content
    ) {
    }

    private record RepositoryName(String owner, String repo) {

        private static RepositoryName from(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("GitHub repository full name is required.");
            }

            String[] parts = fullName.split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("GitHub repository full name must be owner/repo.");
            }
            return new RepositoryName(parts[0], parts[1]);
        }
    }
}
