package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoupangSearchResponse {
    private String rCode;
    private String rMessage;
    private SearchData data;

    @Getter
    @Setter
    public static class SearchData {
        private String landingUrl;
        private List<CoupangProductData> productData;
    }
}
