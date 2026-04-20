package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminCoupangLinkPageResponse {
    private long total;
    private List<AdminCoupangLinkResponse> items;
}
