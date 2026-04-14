package com.example.nutriuniv.domain.brand.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.brand.dto.BrandRequest;
import com.example.nutriuniv.domain.brand.dto.BrandResponse;
import com.example.nutriuniv.domain.brand.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Brand", description = "브랜드 API")
@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // GET /brands
    @Operation(summary = "전체 브랜드 목록 조회",
            description = "활성 상품이 연결된 브랜드 목록과 상품 수를 반환합니다.")
    @GetMapping("/brands")
    public ResponseEntity<CommonResponse<List<BrandResponse>>> getBrands() {
        return ResponseEntity.ok(CommonResponse.success(brandService.getBrands()));
    }

    // GET /admin/brands
    @Operation(summary = "관리자 브랜드 목록 조회",
            description = "키워드 검색 및 페이지네이션을 지원합니다.")
    @GetMapping("/admin/brands")
    public ResponseEntity<CommonResponse<BrandResponse.BrandPageResponse>> getAdminBrands(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(CommonResponse.success(brandService.getAdminBrands(keyword, page, size)));
    }

    // POST /admin/brands
    @Operation(summary = "브랜드 등록",
            description = "새 브랜드를 등록합니다. 중복 브랜드명은 409 반환.")
    @PostMapping("/admin/brands")
    public ResponseEntity<CommonResponse<Long>> createBrand(
            @RequestBody BrandRequest request) {

        return ResponseEntity.ok(CommonResponse.success(brandService.createBrand(request)));
    }

    // PATCH /admin/brands/{brandId}
    @Operation(summary = "브랜드명 수정",
            description = "브랜드명을 수정합니다. 중복 브랜드명은 409 반환.")
    @PatchMapping("/admin/brands/{brandId}")
    public ResponseEntity<CommonResponse<Void>> updateBrand(
            @Parameter(description = "브랜드 ID") @PathVariable Long brandId,
            @RequestBody BrandRequest request) {

        brandService.updateBrand(brandId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // DELETE /admin/brands/{brandId}
    @Operation(summary = "브랜드 삭제",
            description = "브랜드를 소프트 딜리트(is_active=false) 처리합니다. " +
                    "활성 상품이 존재하면 400 반환.")
    @DeleteMapping("/admin/brands/{brandId}")
    public ResponseEntity<CommonResponse<Void>> deleteBrand(
            @Parameter(description = "브랜드 ID") @PathVariable Long brandId) {

        brandService.deleteBrand(brandId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}