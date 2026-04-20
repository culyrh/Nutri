package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdminViewStatResponse {
    private LocalDate date;
    private long viewCount;
    private long uniqueProductCount;
}
