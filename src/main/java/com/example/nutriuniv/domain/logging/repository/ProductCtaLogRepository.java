package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.ProductCtaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ProductCtaLogRepository extends JpaRepository<ProductCtaLog, Long> {

    boolean existsByUserIdAndProduct_IdAndCreatedAtAfter(Long userId, Long productId, LocalDateTime after);
}
