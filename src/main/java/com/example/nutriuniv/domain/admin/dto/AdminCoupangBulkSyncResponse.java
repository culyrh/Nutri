package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCoupangBulkSyncResponse {

    private int totalCount;
    private int successCount;
    private int failCount;
}
