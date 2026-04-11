package com.example.nutriuniv.domain.product.repository;

import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductNutrientRepository extends JpaRepository<ProductNutrient, Long> {
    // @MapsId로 product_id가 PK이므로 findByProductId 불필요
    // findById(productId)로 바로 조회 가능
}