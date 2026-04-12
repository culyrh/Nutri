package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal nutritionScore;
    private int viewCount;
    private boolean isFavorited;
    private Double scoreRankPercent;
    private BrandInfo brand;
    private CategoryInfo category;
    private NutrientInfo nutrients;
    private CoupangInfo coupang;   // 나중에 채울 예정, 지금은 null

    @Getter
    @Builder
    public static class BrandInfo {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    public static class NutrientInfo {
        private String servingSize;
        private BigDecimal calories;
        private BigDecimal carbohydrate;
        private BigDecimal sugar;
        private BigDecimal protein;
        private BigDecimal fat;
        private BigDecimal saturatedFat;
        private BigDecimal transFat;
        private BigDecimal cholesterol;
        private BigDecimal sodium;
    }

    @Getter
    @Builder
    public static class CoupangInfo {
        private String affiliateUrl;
        private String landingUrl;
        private Integer price;
        private Boolean isRocket;
        private Boolean isFreeShipping;
        private LocalDateTime lastSyncedAt;
    }
}