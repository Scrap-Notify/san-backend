package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GithubRepositoryConnectionRepository extends JpaRepository<GithubRepositoryConnection, UUID> {

    List<GithubRepositoryConnection> findAllByUser_UserId(UUID userId);

    Optional<GithubRepositoryConnection> findByUser_UserIdAndGithubRepositoryId(UUID userId, Long githubRepositoryId);
}
