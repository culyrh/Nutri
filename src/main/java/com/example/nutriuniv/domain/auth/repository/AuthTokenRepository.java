package com.example.nutriuniv.domain.auth.repository;

import com.example.nutriuniv.domain.auth.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByRefreshToken(String refreshToken);
    void deleteByUserId(Long userId);
}