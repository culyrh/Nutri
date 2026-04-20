package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminUserPageResponse {
    private long total;
    private int page;
    private int size;
    private List<AdminUserResponse> items;
}
