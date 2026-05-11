package com.example.nutriuniv.domain.product.entity;

import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.category.entity.Category;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private ProductNutrient productNutrient;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private CoupangLink coupangLink;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "nutrition_score", precision = 5, scale = 2)
    private BigDecimal nutritionScore;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // pgvector 타입 (10차원). Python vectorize.py가 직접 write한다.
    // JPA INSERT/UPDATE에서 제외 — Python만 이 컬럼을 관리함
    @Column(name = "nutrient_vector", columnDefinition = "vector(10)", insertable = false, updatable = false)
    private String nutrientVector;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Product create(String name, Category category, Brand brand) {
        Product p = new Product();
        p.name = name;
        p.category = category;
        p.brand = brand;
        return p;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void update(Category category, Brand brand) {
        this.category = category;
        this.brand = brand;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void update(String name, Category category, Brand brand,
                       String imageUrl, BigDecimal nutritionScore) {
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.nutritionScore = nutritionScore;
    }
}