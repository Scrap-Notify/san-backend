package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** TIL GitHub 커밋 이력을 조회하는 repository */
public interface TilGithubCommitRepository extends JpaRepository<TilGithubCommit, UUID> {

    boolean existsByGithubRepositoryConnection_GithubRepositoryConnectionIdAndBranchAndContentHashAndStatus(
            UUID githubRepositoryConnectionId,
            String branch,
            String contentHash,
            TilGithubCommitStatus status
    );
}
