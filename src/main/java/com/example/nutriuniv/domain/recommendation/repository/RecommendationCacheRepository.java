package com.example.nutriuniv.domain.recommendation.repository;

import com.example.nutriuniv.domain.recommendation.entity.RecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {

    List<RecommendationCache> findByUserIdOrderByScoreDesc(Long userId);
}
