package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GithubAccountRepository extends JpaRepository<GithubAccount, UUID> {

    Optional<GithubAccount> findByUser_UserId(UUID userId);

    Optional<GithubAccount> findByGithubUserId(String githubUserId);
}
