package com.example.nutriuniv.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductPageResponse {
    // 목록 조회 응답 전체 (total, page, size, items)
    private long total;
    private int page;
    private int size;
    private List<ProductListResponse> items;
}