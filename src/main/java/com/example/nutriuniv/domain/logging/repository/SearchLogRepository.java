package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    // 관리자 검색 통계 (일별 검색수 / 고유 키워드 수)
    @Query(value = """
            SELECT DATE(searched_at)          AS date,
                   COUNT(*)                  AS search_count,
                   COUNT(DISTINCT keyword)   AS unique_keyword_count
            FROM search_logs
            WHERE searched_at >= :start AND searched_at < :end
            GROUP BY DATE(searched_at)
            ORDER BY DATE(searched_at)
            """, nativeQuery = true)
    List<Object[]> findDailyStats(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);
}
