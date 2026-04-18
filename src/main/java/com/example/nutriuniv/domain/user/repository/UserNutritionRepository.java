package com.example.nutriuniv.domain.user.repository;

import com.example.nutriuniv.domain.user.entity.UserNutrition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNutritionRepository extends JpaRepository<UserNutrition, Long> {
    Optional<UserNutrition> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}