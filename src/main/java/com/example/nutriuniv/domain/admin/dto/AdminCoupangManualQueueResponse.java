package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 수동 매핑 작업 큐. productId 오름차순 정렬된 작업 대상 목록.
 */
@Getter
@Builder
public class AdminCoupangManualQueueResponse {
    /** 작업 대상 productId 총 개수 */
    private int totalCount;

    /** 작업 대상 productId (오름차순) */
    private List<Long> productIds;

    /** 필터 조건 echo (디버그/표시용) */
    private List<String> statuses;
    private Long fromId;
}
