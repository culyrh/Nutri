package com.example.nutriuniv.domain.pns.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "product_pns_by_eer",
       indexes = {
           @Index(name = "idx_pns_band_grade",      columnList = "eer_band, grade"),
           @Index(name = "idx_pns_band_percentile", columnList = "eer_band, percentile DESC")
       })
@IdClass(ProductPnsByEer.PnsId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPnsByEer {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Id
    @Column(name = "eer_band")
    private Integer eerBand;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal score;

    @Column(length = 1, nullable = false)
    private String grade;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentile;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ProductPnsByEer create(Long productId, int eerBand,
                                         double score, String grade) {
        ProductPnsByEer p = new ProductPnsByEer();
        p.productId = productId;
        p.eerBand = eerBand;
        p.score = sanitize(score);
        p.grade = grade;
        p.updatedAt = LocalDateTime.now();
        return p;
    }

    public void updateScore(double score, String grade) {
        this.score = sanitize(score);
        this.grade = grade;
        this.updatedAt = LocalDateTime.now();
    }

    /** 데이터 이상치(-∞급 점수)로 BigDecimal 변환 실패하는 케이스 방지. precision=10 한도(±9,999,999)에서 안전. */
    private static BigDecimal sanitize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return BigDecimal.ZERO;
        double clamped = Math.max(-9_999_999.0, Math.min(9_999_999.0, v));
        return BigDecimal.valueOf(clamped).setScale(3, java.math.RoundingMode.HALF_UP);
    }

    public void updatePercentile(double percentile) {
        this.percentile = BigDecimal.valueOf(percentile).setScale(2, java.math.RoundingMode.HALF_UP);
        this.updatedAt = LocalDateTime.now();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PnsId implements Serializable {
        private Long productId;
        private Integer eerBand;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PnsId that)) return false;
            return Objects.equals(productId, that.productId)
                && Objects.equals(eerBand,   that.eerBand);
        }
        @Override
        public int hashCode() {
            return Objects.hash(productId, eerBand);
        }
    }
}
