package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdminSearchStatResponse {
    private LocalDate date;
    private long searchCount;
    private long uniqueKeywordCount;
}
