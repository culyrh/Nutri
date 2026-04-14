package com.example.nutriuniv.domain.brand.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BrandResponse {

    private Long id;
    private String name;
    private long productCount;

    // 관리자 페이지 응답
    @Getter
    @Builder
    public static class BrandPageResponse {
        private long total;
        private int page;
        private int size;
        private List<BrandResponse> items;
    }
}