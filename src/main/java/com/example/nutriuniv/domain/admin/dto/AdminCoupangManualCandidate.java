package com.example.nutriuniv.domain.admin.dto;

import com.example.nutriuniv.domain.coupang.dto.CoupangProductData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 쿠팡 검색 후보 한 건. CLI 에 번호와 함께 표시되고, 사용자가 선택하면 그대로 서버에 다시 보내짐.
 * 서버는 클라이언트가 들고 있던 candidate 데이터를 그대로 받아 매핑에 사용 (stateless).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCoupangManualCandidate {
    private int    index;             // 1-based 번호 (사용자가 입력하는 값)
    private String productId;
    private String productName;
    private String productImage;
    private Integer productPrice;
    private Boolean isRocket;
    private Boolean isFreeShipping;
    private String productUrl;        // affiliate 트래킹 포함 URL

    public static AdminCoupangManualCandidate of(int index, CoupangProductData data) {
        return AdminCoupangManualCandidate.builder()
                .index(index)
                .productId(data.getProductId() == null ? null : String.valueOf(data.getProductId()))
                .productName(data.getProductName())
                .productImage(data.getProductImage())
                .productPrice(data.getProductPrice())
                .isRocket(data.getIsRocket())
                .isFreeShipping(data.getIsFreeShipping())
                .productUrl(data.getProductUrl())
                .build();
    }
}
