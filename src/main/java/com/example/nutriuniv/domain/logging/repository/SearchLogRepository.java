package com.example.nutriuniv.domain.logging.repository;

import com.example.nutriuniv.domain.logging.entity.SearchLog;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import org.springframework.data.domain.Pageable;
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

    // keyword 기준으로 중복 제거 후, 가장 최근 searched_at 기준 내림차순 정렬, 최대 10개
    @Query("""
            SELECT new com.example.nutriuniv.domain.search.dto.RecentKeywordResponse(
                s.keyword, MAX(s.searchedAt)
            )
            FROM SearchLog s
            WHERE s.userId = :userId
            GROUP BY s.keyword
            ORDER BY MAX(s.searchedAt) DESC
            """)
    List<RecentKeywordResponse> findRecentKeywords(@Param("userId") Long userId, Pageable pageable);
}