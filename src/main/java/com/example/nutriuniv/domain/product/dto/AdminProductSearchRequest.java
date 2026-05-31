package com.example.nutriuniv.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminProductSearchRequest {

    private String keyword;
    private Boolean isActive;       // null = 전체, true = 활성, false = 비활성
    private List<Long> categoryIds;
    private List<Long> brandIds;
    private String linkStatus;      // LINKED / UNLINKED / FAILED / null = 전체

    private int page = 0;
    private int size = 20;
}