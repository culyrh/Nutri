package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoupangProductData {
    private Long productId;
    private String productName;
    private String productImage;
    private Integer productPrice;
    private Boolean isRocket;
    private Boolean isFreeShipping;
    private String productUrl;   // 파트너스 트래킹 포함 → affiliateUrl로 사용
}
