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
    private PnsInfo pns;            // 점수 미계산 상품이면 null

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

    @Getter
    @Builder
    public static class PnsInfo {
        private BigDecimal score;            // PFS 원점수 (-∞ ~ +9)
        private String grade;                // A~E
        private BigDecimal percentile;       // 카테고리 내 백분위 (0~100, 클수록 좋음)
        private BigDecimal topPercent;       // 상위 X% (= 100 - percentile)
        private Long parentCategoryId;       // 대분류 ID
        private String parentCategoryName;   // 대분류 이름 (예: "음료류")
        private int categoryTotal;           // 대분류 안 활성 상품 수
        private int eerBand;                 // 사용된 EER 구간 (1500/2000/2500/3000)
    }
}