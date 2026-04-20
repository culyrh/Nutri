package com.example.nutriuniv.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecentKeywordResponse {
    private String keyword;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime searchedAt;

    // JPQL new 생성자용
    public RecentKeywordResponse(String keyword, LocalDateTime searchedAt) {
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }
}
