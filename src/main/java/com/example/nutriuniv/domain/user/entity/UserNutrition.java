package com.example.nutriuniv.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_nutrition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "body_fat_rate", precision = 5, scale = 2)
    private BigDecimal bodyFatRate;

    @Column(name = "skeletal_muscle_mass", precision = 5, scale = 2)
    private BigDecimal skeletalMuscleMass;

    @Column(name = "diet_purpose", nullable = false, length = 20)
    private String dietPurpose;

    @Column(name = "activity_type", nullable = false, length = 20)
    private String activityType;

    @Column(name = "weekly_exercise_count", nullable = false)
    private int weeklyExerciseCount;

    @Column(name = "exercise_intensity", nullable = false, length = 10)
    private String exerciseIntensity;

    @Column(name = "daily_meal_count", nullable = false)
    private int dailyMealCount;

    @Column(name = "daily_snack_count", nullable = false)
    private int dailySnackCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserNutrition create(User user, BigDecimal height, BigDecimal weight,
                                       BigDecimal bodyFatRate, BigDecimal skeletalMuscleMass,
                                       String dietPurpose, String activityType,
                                       int weeklyExerciseCount, String exerciseIntensity,
                                       int dailyMealCount, int dailySnackCount) {
        UserNutrition n = new UserNutrition();
        n.user = user;
        n.height = height;
        n.weight = weight;
        n.bodyFatRate = bodyFatRate;
        n.skeletalMuscleMass = skeletalMuscleMass;
        n.dietPurpose = dietPurpose;
        n.activityType = activityType;
        n.weeklyExerciseCount = weeklyExerciseCount;
        n.exerciseIntensity = exerciseIntensity;
        n.dailyMealCount = dailyMealCount;
        n.dailySnackCount = dailySnackCount;
        return n;
    }

    public void update(BigDecimal height, BigDecimal weight,
                       BigDecimal bodyFatRate, BigDecimal skeletalMuscleMass,
                       String dietPurpose, String activityType,
                       int weeklyExerciseCount, String exerciseIntensity,
                       int dailyMealCount, int dailySnackCount) {
        this.height = height;
        this.weight = weight;
        this.bodyFatRate = bodyFatRate;
        this.skeletalMuscleMass = skeletalMuscleMass;
        this.dietPurpose = dietPurpose;
        this.activityType = activityType;
        this.weeklyExerciseCount = weeklyExerciseCount;
        this.exerciseIntensity = exerciseIntensity;
        this.dailyMealCount = dailyMealCount;
        this.dailySnackCount = dailySnackCount;
    }
}