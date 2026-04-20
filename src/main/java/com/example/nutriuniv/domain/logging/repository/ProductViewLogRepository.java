package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductViewLogRepository extends JpaRepository<ProductViewLog, Long> {

    // 관리자 조회 통계 (일별 조회수 / 고유 상품 수)
    @Query(value = """
            SELECT DATE(created_at)        AS date,
                   COUNT(*)               AS view_count,
                   COUNT(DISTINCT product_id) AS unique_product_count
            FROM product_view_logs
            WHERE created_at >= :start AND created_at < :end
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """, nativeQuery = true)
    List<Object[]> findDailyStats(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);
}
