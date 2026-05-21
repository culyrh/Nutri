package com.example.nutriuniv.domain.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수동 매핑 액션 요청.
 *   action = "SELECT" → candidate 필드 필요. 해당 후보로 LINKED 처리 (기존 매핑 덮어쓰기 포함)
 *   action = "SKIP"   → 영구 SKIPPED 처리. candidate 무시
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminCoupangManualActionRequest {
    private String action;                         // "SELECT" or "SKIP"
    private AdminCoupangManualCandidate candidate;  // SELECT 시 필수
    private String searchKeyword;                  // 어떤 키워드로 찾은 결과인지 기록용
    private String landingUrl;                     // SELECT 시 함께 저장
}
