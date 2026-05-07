package com.example.nutriuniv.domain.admin.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangBulkSyncResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangLinkPageResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangSyncResponse;
import com.example.nutriuniv.domain.admin.service.AdminCoupangService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
