package com.example.nutriuniv.domain.search.repository;

import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import com.example.nutriuniv.domain.search.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

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
