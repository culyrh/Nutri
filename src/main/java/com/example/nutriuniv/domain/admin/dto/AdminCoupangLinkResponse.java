package com.example.nutriuniv.domain.admin.dto;

import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCoupangLinkResponse {
    private Long productId;
    private String productName;
    private String coupangProductName;
    private String linkStatus;
    private Integer price;
    private Boolean isRocket;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;

    public static AdminCoupangLinkResponse from(CoupangLink link) {
        return AdminCoupangLinkResponse.builder()
                .productId(link.getProduct().getId())
                .productName(link.getProduct().getName())
                .coupangProductName(link.getCoupangProductName())
                .linkStatus(link.getLinkStatus())
                .price(link.getProductPrice())
                .isRocket(link.getIsRocket())
                .lastSyncedAt(link.getLastSyncedAt())
                .build();
    }
}
