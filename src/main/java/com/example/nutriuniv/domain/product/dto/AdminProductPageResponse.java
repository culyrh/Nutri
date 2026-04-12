package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AdminProductPageResponse {

    private long total;
    private int page;
    private int size;
    private List<AdminProductListResponse> items;

    @Getter
    @Builder
    public static class AdminProductListResponse {
        private Long id;
        private String name;
        private BrandInfo brand;
        private CategoryInfo category;
        private boolean isActive;
        private String linkStatus;      // LINKED / UNLINKED / FAILED / null (쿠팡 미구현)
        private BigDecimal nutritionScore;
    }

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
}