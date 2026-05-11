package com.example.nutriuniv.domain.recommendation.repository;

import com.example.nutriuniv.domain.recommendation.entity.ProductVectorByDiet;
import com.example.nutriuniv.domain.recommendation.entity.ProductVectorByDietId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVectorByDietRepository extends JpaRepository<ProductVectorByDiet, ProductVectorByDietId> {

    // diet_purpose 기준으로 타겟 벡터와 코사인 거리가 가까운 제품 ID 순서대로 반환
    @Query(value = """
            SELECT pvbd.product_id
            FROM product_vectors_by_diet pvbd
            JOIN products p ON pvbd.product_id = p.id
            WHERE pvbd.diet_purpose = :dietPurpose
              AND p.is_active = true
            ORDER BY pvbd.weighted_vector <=> CAST(:targetVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTopProductIdsByDietPurpose(
            @Param("dietPurpose") String dietPurpose,
            @Param("targetVector") String targetVector,
            @Param("limit") int limit
    );
}
