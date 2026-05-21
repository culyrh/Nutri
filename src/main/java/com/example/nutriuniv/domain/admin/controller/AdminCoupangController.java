package com.example.nutriuniv.domain.admin.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangBulkSyncResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangLinkPageResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangManualActionRequest;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangManualActionResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangManualQueueResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangManualSearchResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangSyncResponse;
import com.example.nutriuniv.domain.admin.service.AdminCoupangService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Coupang", description = "관리자 쿠팡 연동 API")
@RestController
@RequiredArgsConstructor
public class AdminCoupangController {

    private final AdminCoupangService adminCoupangService;

    // GET /admin/coupang/links
    @Operation(summary = "쿠팡 연동 목록 조회",
            description = "status 미입력 시 전체 조회. 허용 값: LINKED / UNLINKED / FAILED")
    @GetMapping("/admin/coupang/links")
    public ResponseEntity<CommonResponse<AdminCoupangLinkPageResponse>> getCoupangLinks(
            @Parameter(description = "연동 상태 필터 (LINKED / UNLINKED / FAILED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.getCoupangLinks(status, page, size)));
    }

    // POST /admin/coupang/sync
    @Operation(summary = "쿠팡 링크 일괄 매핑",
            description = "UNLINKED 상태인 전체 상품을 쿠팡 파트너스 API로 매핑합니다. " +
                    "API 제한(분당 50회) 대비 여유있게 2초 간격으로 처리합니다.")
    @PostMapping("/admin/coupang/sync")
    public ResponseEntity<CommonResponse<AdminCoupangBulkSyncResponse>> bulkSyncCoupangLinks() {
        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.bulkSyncCoupangLinks()));
    }

    // POST /admin/coupang/retry
    @Operation(summary = "쿠팡 링크 일괄 재시도",
            description = "FAILED 상태인 전체 상품을 재시도합니다. 2초 간격으로 처리합니다.")
    @PostMapping("/admin/coupang/retry")
    public ResponseEntity<CommonResponse<AdminCoupangBulkSyncResponse>> retryCoupangLinks() {
        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.retryCoupangLinks()));
    }

    // POST /admin/coupang/sync/{productId}
    @Operation(summary = "쿠팡 링크 단건 매핑",
            description = "특정 상품의 쿠팡 파트너스 링크를 매핑합니다. " +
                    "API 실패 시 link_status=FAILED로 저장됩니다.")
    @PostMapping("/admin/coupang/sync/{productId}")
    public ResponseEntity<CommonResponse<AdminCoupangSyncResponse>> syncCoupangLink(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {

        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.syncCoupangLink(productId)));
    }

    // ── 수동 매핑 (Manual Mapping) ────────────────────────────────────────────────
    // CLI 도구(scripts/manual_map.py) 와 함께 사용

    // GET /admin/coupang/manual/queue
    @Operation(summary = "수동 매핑 작업 큐 조회",
            description = "조건에 맞는 productId 를 오름차순으로 반환합니다. " +
                    "status 미지정 시 'FAILED' 만 (기본). " +
                    "여러 status 동시 지정 가능 (예: status=FAILED&status=UNLINKED). " +
                    "'ALL' 이 포함되면 전체.")
    @GetMapping("/admin/coupang/manual/queue")
    public ResponseEntity<CommonResponse<AdminCoupangManualQueueResponse>> getManualQueue(
            @Parameter(description = "필터 상태 (LINKED / UNLINKED / FAILED / SKIPPED / ALL). 미지정 시 FAILED.")
            @RequestParam(required = false) List<String> status,
            @Parameter(description = "이 productId 이상부터 (재개용)")
            @RequestParam(required = false, defaultValue = "0") Long fromId) {

        List<String> effective = (status == null || status.isEmpty()) ? List.of("FAILED") : status;
        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.getManualQueue(effective, fromId)));
    }

    // GET /admin/coupang/manual?productId=X&keyword=Y
    @Operation(summary = "수동 매핑용 상품+쿠팡 후보 조회",
            description = "지정된 productId 의 상품 정보, 현재 매핑 상태, 쿠팡 검색 후보(최대 20개)를 한 번에 반환합니다. " +
                    "keyword 미지정 시 product.name 으로 검색.")
    @GetMapping("/admin/coupang/manual")
    public ResponseEntity<CommonResponse<AdminCoupangManualSearchResponse>> getManualSearch(
            @Parameter(description = "상품 ID") @RequestParam Long productId,
            @Parameter(description = "검색 키워드 (미지정 시 product.name 사용)")
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.getManualSearch(productId, keyword)));
    }

    // POST /admin/coupang/manual/{productId}
    @Operation(summary = "수동 매핑 액션 처리",
            description = "action=SELECT 면 candidate 정보로 매핑 확정 (덮어쓰기 포함). " +
                    "action=SKIP 면 SKIPPED 상태로 영구 표시 (다음 큐에서 제외).")
    @PostMapping("/admin/coupang/manual/{productId}")
    public ResponseEntity<CommonResponse<AdminCoupangManualActionResponse>> processManualAction(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @org.springframework.web.bind.annotation.RequestBody AdminCoupangManualActionRequest request) {

        return ResponseEntity.ok(CommonResponse.success(
                adminCoupangService.processManualAction(productId, request)));
    }
}
