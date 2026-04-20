package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewLogRepository extends JpaRepository<ProductViewLog, Long> {
}
