package com.example.nutriuniv.domain.admin.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.admin.dto.AdminUserPageResponse;
import com.example.nutriuniv.domain.admin.dto.AdminUserRoleRequest;
import com.example.nutriuniv.domain.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - User", description = "관리자 유저 API")
@RestController
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // GET /admin/users
    @Operation(summary = "유저 목록 조회",
            description = "keyword로 이름/이메일 검색. 미입력 시 전체 조회.")
    @GetMapping("/admin/users")
    public ResponseEntity<CommonResponse<AdminUserPageResponse>> getUsers(
            @Parameter(description = "검색 키워드 (이름 또는 이메일)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(CommonResponse.success(adminUserService.getUsers(keyword, page, size)));
    }

    // PATCH /admin/users/{userId}/role
    @Operation(summary = "유저 role 변경",
            description = "허용 값: USER / ADMIN. 본인 role 변경 시 403 반환.")
    @PatchMapping("/admin/users/{userId}/role")
    public ResponseEntity<CommonResponse<Void>> updateUserRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "대상 유저 ID") @PathVariable Long userId,
            @RequestBody AdminUserRoleRequest request) {

        adminUserService.updateUserRole(principal.getId(), userId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
