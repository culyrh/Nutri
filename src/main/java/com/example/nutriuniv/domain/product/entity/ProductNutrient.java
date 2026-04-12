package com.example.nutriuniv.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_nutrients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProductNutrient {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "serving_size", length = 50)
    private String servingSize;

    @Column(precision = 8, scale = 3)
    private BigDecimal calories;

    @Column(precision = 8, scale = 3)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 3)
    private BigDecimal sugar;

    @Column(precision = 8, scale = 3)
    private BigDecimal protein;

    @Column(precision = 8, scale = 3)
    private BigDecimal fat;

    @Column(name = "saturated_fat", precision = 8, scale = 3)
    private BigDecimal saturatedFat;

    @Column(name = "trans_fat", precision = 8, scale = 3)
    private BigDecimal transFat;

    @Column(precision = 8, scale = 3)
    private BigDecimal cholesterol;

    @Column(precision = 8, scale = 3)
    private BigDecimal sodium;

    @Column(name = "dietary_fiber", precision = 8, scale = 3)
    private BigDecimal fiber;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 정적 팩토리
    public static ProductNutrient create(Product product) {
        ProductNutrient n = new ProductNutrient();
        n.product = product;
        return n;
    }

    // 업데이트
    public void update(String servingSize,
                       BigDecimal calories, BigDecimal carbohydrate, BigDecimal sugar,
                       BigDecimal protein, BigDecimal fat, BigDecimal saturatedFat,
                       BigDecimal transFat, BigDecimal cholesterol, BigDecimal sodium,
                       BigDecimal fiber) {
        this.servingSize  = servingSize;
        this.calories     = calories;
        this.carbohydrate = carbohydrate;
        this.sugar        = sugar;
        this.protein      = protein;
        this.fat          = fat;
        this.saturatedFat = saturatedFat;
        this.transFat     = transFat;
        this.cholesterol  = cholesterol;
        this.sodium       = sodium;
        this.fiber        = fiber;
    }
}