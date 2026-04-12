package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductUploadResponse {

    private int totalCount;
    private int successCount;
    private int failCount;
    private List<FailItem> failItems;

    @Getter
    @Builder
    public static class FailItem {
        private int rowNumber;      // 엑셀 행 번호 (헤더 제외, 2부터 시작)
        private String productName;
        private String reason;
    }
}