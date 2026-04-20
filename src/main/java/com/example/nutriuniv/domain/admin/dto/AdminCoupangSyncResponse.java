package com.example.nutriuniv.domain.admin.dto;

import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCoupangSyncResponse {
    private String linkStatus;
    private String affiliateUrl;
    private Integer price;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;

    public static AdminCoupangSyncResponse from(CoupangLink link) {
        return AdminCoupangSyncResponse.builder()
                .linkStatus(link.getLinkStatus())
                .affiliateUrl(link.getAffiliateUrl())
                .price(link.getProductPrice())
                .lastSyncedAt(link.getLastSyncedAt())
                .build();
    }
}
