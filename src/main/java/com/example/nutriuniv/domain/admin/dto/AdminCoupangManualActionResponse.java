package com.example.nutriuniv.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 수동 매핑 액션 결과. CLI 에 "✓ 매핑 완료" 표시할 때 사용.
 */
@Getter
@Builder
public class AdminCoupangManualActionResponse {
    private Long   productId;
    private String linkStatus;             // 변경 후 상태 (LINKED / SKIPPED)
    private String coupangProductName;     // LINKED 시 매핑된 상품명
    private String message;                // 사용자 표시용 메시지
}
