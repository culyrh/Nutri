package com.example.nutriuniv.domain.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class ProductVectorByDietId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "diet_purpose", nullable = false, length = 20)
    private String dietPurpose;

    public ProductVectorByDietId(Long productId, String dietPurpose) {
        this.productId = productId;
        this.dietPurpose = dietPurpose;
    }
}
