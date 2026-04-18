package com.example.nutriuniv.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UserNutritionRequest {
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bodyFatRate;
    private BigDecimal skeletalMuscleMass;
    private String dietPurpose;
    private String activityType;
    private int weeklyExerciseCount;
    private String exerciseIntensity;
    private int dailyMealCount;
    private int dailySnackCount;
}