package com.example.nutriuniv.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 30)
    private String keyword;

    @Column(name = "result_count", nullable = false)
    private int resultCount = 0;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    public static SearchLog create(Long userId, String keyword, int resultCount) {
        SearchLog log = new SearchLog();
        log.userId = userId;
        log.keyword = keyword;
        log.resultCount = resultCount;
        log.searchedAt = LocalDateTime.now();
        return log;
    }
}
