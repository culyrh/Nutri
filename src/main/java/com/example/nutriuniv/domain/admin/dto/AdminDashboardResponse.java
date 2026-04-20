package com.example.nutriuniv.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminDashboardResponse {

    private long totalProducts;
    private CategoryStats totalCategories;
    private BigDecimal avgNutritionScore;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdatedAt;

    private CoupangLinkStats coupangLinkStatus;

    @Getter
    @Builder
    public static class CategoryStats {
        private long depth1;
        private long depth2;
        private long depth3;
    }

    @Getter
    @Builder
    public static class CoupangLinkStats {
        private long total;
        private long linked;
        private long unlinked;
        private long failed;
    }
}
