package com.example.nutriuniv.domain.logging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SearchLogRequest {
    private String keyword;
    private int resultCount;
}
