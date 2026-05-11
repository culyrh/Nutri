package com.example.nutriuniv.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_vectors_by_diet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVectorByDiet {

    @EmbeddedId
    private ProductVectorByDietId id;

    // pgvector 타입. Python vectorize.py가 직접 write하고 추천 API가 SQL로 read한다.
    @Column(name = "weighted_vector", nullable = false, columnDefinition = "vector(10)")
    private String weightedVector;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
