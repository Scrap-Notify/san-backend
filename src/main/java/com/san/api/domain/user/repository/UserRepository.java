package com.san.api.domain.user.repository;

import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByUsername(String username);
}
