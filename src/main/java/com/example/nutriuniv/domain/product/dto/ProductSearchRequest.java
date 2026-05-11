package com.example.nutriuniv.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequest {

    private String keyword;
    private Long categoryId;
    private Long brandId;

    // 칼로리
    private BigDecimal minCalories;
    private BigDecimal maxCalories;

    // 단백질
    private BigDecimal minProtein;
    private BigDecimal maxProtein;

    // 지방
    private BigDecimal minFat;
    private BigDecimal maxFat;

    // 탄수화물
    private BigDecimal minCarbohydrate;
    private BigDecimal maxCarbohydrate;

    // 당
    private BigDecimal minSugar;
    private BigDecimal maxSugar;

    // 나트륨
    private BigDecimal minSodium;
    private BigDecimal maxSodium;

    // 영양점수
    private BigDecimal minNutritionScore;
    private BigDecimal maxNutritionScore;

    private String sort;   // POPULAR/SCORE/ACCURACY/RECOMMENDED

    private int page = 0;
    private int size = 20;
}