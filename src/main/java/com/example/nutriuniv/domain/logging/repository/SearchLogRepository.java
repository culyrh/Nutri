package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
