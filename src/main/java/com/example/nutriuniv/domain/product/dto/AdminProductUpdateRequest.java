package com.example.nutriuniv.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AdminProductUpdateRequest {

    private String name;
    private Long categoryId;
    private Long brandId;
    private Boolean isActive;
    private String imageUrl;
    private BigDecimal nutritionScore;
}