package com.example.nutriuniv.domain.product.repository;

import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    // 브랜드에 상품 존재 시 삭제 차단
    boolean existsByBrandAndIsActiveTrue(Brand brand);

    // 카테고리에 상품 존재 시 삭제 차단
    boolean existsByCategoryIdAndIsActiveTrue(Long categoryId);

    // 카테고리별 브랜드 목록 + 상품 수 (GET /categories/{id}/brands)
    @Query("SELECT p.brand, COUNT(p) FROM Product p " +
            "WHERE p.category.id = :categoryId AND p.isActive = true AND p.brand IS NOT NULL " +
            "GROUP BY p.brand")
    List<Object[]> findBrandCountByCategoryId(@Param("categoryId") Long categoryId);

    // 전체 브랜드별 상품 수 (GET /brands)
    @Query("SELECT p.brand, COUNT(p) FROM Product p " +
            "WHERE p.isActive = true AND p.brand IS NOT NULL " +
            "GROUP BY p.brand")
    List<Object[]> findBrandCountAll();
}

