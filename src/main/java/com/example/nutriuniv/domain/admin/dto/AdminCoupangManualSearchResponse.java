package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 한 productId 에 대한 수동 매핑 화면용 응답.
 * - product: 우리 DB 상품 정보
 * - currentMapping: 이미 매핑된 게 있으면 쿠팡 상품명 + 상태 (없으면 null)
 * - candidates: 이번 검색의 쿠팡 후보 (최대 20개)
 * - landingUrl: 쿠팡 검색 결과 공통 affiliate landing URL (매핑 시 함께 저장)
 */
@Getter
@Builder
public class AdminCoupangManualSearchResponse {

    @Getter
    @Builder
    public static class ProductInfo {
        private Long   id;
        private String name;
    }

    @Getter
    @Builder
    public static class CurrentMapping {
        private String linkStatus;
        private String coupangProductName;
        private String coupangProductId;
    }

    private ProductInfo                   product;
    private CurrentMapping                currentMapping;   // 매핑 없으면 null
    private String                        searchKeyword;    // 실제 사용된 검색 키워드
    private String                        landingUrl;
    private List<AdminCoupangManualCandidate> candidates;
}
