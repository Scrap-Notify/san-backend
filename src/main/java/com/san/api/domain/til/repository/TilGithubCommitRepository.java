package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

/** TIL GitHub 커밋 이력을 조회하는 repository */
public interface TilGithubCommitRepository extends JpaRepository<TilGithubCommit, UUID> {

    @Query("""
            SELECT COUNT(tgc) > 0
            FROM TilGithubCommit tgc
            WHERE tgc.githubRepositoryConnection.githubRepositoryConnectionId = :githubRepositoryConnectionId
              AND tgc.branch = :branch
              AND tgc.contentHash = :contentHash
              AND tgc.status IN :statuses
            """)
    boolean existsDuplicateContent(
            @Param("githubRepositoryConnectionId") UUID githubRepositoryConnectionId,
            @Param("branch") String branch,
            @Param("contentHash") String contentHash,
            @Param("statuses") Collection<TilGithubCommitStatus> statuses
    );
}
