package com.example.nutriuniv.domain.coupang.entity;

import com.example.nutriuniv.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupang_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CoupangLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "coupang_product_id", length = 100)
    private String coupangProductId;

    @Column(name = "affiliate_url", length = 1000)
    private String affiliateUrl;

    @Column(name = "landing_url", length = 1000)
    private String landingUrl;

    @Column(name = "search_keyword", nullable = false, length = 255)
    private String searchKeyword;

    @Column(name = "coupang_product_name", length = 255)
    private String coupangProductName;

    @Column(name = "coupang_image_url", length = 500)
    private String coupangImageUrl;

    @Column(name = "product_price")
    private Integer productPrice;

    @Column(name = "is_rocket")
    private Boolean isRocket;

    @Column(name = "is_free_shipping")
    private Boolean isFreeShipping;

    @Column(name = "link_status", nullable = false, length = 10)
    private String linkStatus = "UNLINKED";

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CoupangLink createDefault(Product product) {
        CoupangLink link = new CoupangLink();
        link.product = product;
        link.searchKeyword = product.getName();
        link.linkStatus = "UNLINKED";
        return link;
    }

    public void syncSuccess(String coupangProductId, String coupangProductName,
                            String affiliateUrl, String landingUrl, String coupangImageUrl,
                            Integer productPrice, Boolean isRocket, Boolean isFreeShipping) {
        this.coupangProductId = coupangProductId;
        this.coupangProductName = coupangProductName;
        this.affiliateUrl = affiliateUrl;
        this.landingUrl = landingUrl;
        this.coupangImageUrl = coupangImageUrl;
        this.productPrice = productPrice;
        this.isRocket = isRocket;
        this.isFreeShipping = isFreeShipping;
        this.linkStatus = "LINKED";
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void syncFailed() {
        this.linkStatus = "FAILED";
        this.lastSyncedAt = LocalDateTime.now();
    }

    /** 수동 매핑에서 "매칭 포기" 처리. 다음 작업 큐에서 영구 제외됨. */
    public void markSkipped(String searchKeyword) {
        this.searchKeyword      = searchKeyword == null ? this.searchKeyword : searchKeyword;
        this.linkStatus         = "SKIPPED";
        this.coupangProductId   = null;
        this.coupangProductName = null;
        this.affiliateUrl       = null;
        this.landingUrl         = null;
        this.coupangImageUrl    = null;
        this.productPrice       = null;
        this.isRocket           = null;
        this.isFreeShipping     = null;
        this.lastSyncedAt       = LocalDateTime.now();
    }

    /** 수동 매핑에서 사용자가 선택한 후보로 매핑 확정. searchKeyword 도 같이 기록. */
    public void manualSelect(String coupangProductId, String coupangProductName,
                             String affiliateUrl, String landingUrl, String coupangImageUrl,
                             Integer productPrice, Boolean isRocket, Boolean isFreeShipping,
                             String searchKeyword) {
        this.searchKeyword     = searchKeyword == null ? this.searchKeyword : searchKeyword;
        syncSuccess(coupangProductId, coupangProductName, affiliateUrl, landingUrl,
                    coupangImageUrl, productPrice, isRocket, isFreeShipping);
    }
}
