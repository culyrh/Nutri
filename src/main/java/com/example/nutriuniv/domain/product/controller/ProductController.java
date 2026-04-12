package com.example.nutriuniv.domain.product.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.product.dto.*;
import com.example.nutriuniv.domain.product.service.ProductExcelService;
import com.example.nutriuniv.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Product", description = "상품 API")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductExcelService productExcelService;

    // GET /products
    @Operation(summary = "상품 목록 조회",
            description = "키워드, 카테고리, 브랜드, 영양소 범위 필터로 상품 목록을 조회합니다.")
    @GetMapping("/products")
    public ResponseEntity<CommonResponse<ProductPageResponse>> getProducts(
            @ModelAttribute ProductSearchRequest request) {

        Long userId = null;   // TODO: 인증 구현 후 SecurityContext에서 꺼낼 예정
        return ResponseEntity.ok(CommonResponse.success(productService.getProducts(request, userId)));
    }

    // GET /products/{productId}
    @Operation(summary = "상품 상세 조회",
            description = "상품 ID로 상세 정보를 조회합니다. 조회 시 view_count가 1 증가합니다.")
    @GetMapping("/products/{productId}")
    public ResponseEntity<CommonResponse<ProductDetailResponse>> getProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {

        Long userId = null;   // TODO: 인증 구현 후 SecurityContext에서 꺼낼 예정
        return ResponseEntity.ok(CommonResponse.success(productService.getProduct(productId, userId)));
    }

    // GET /admin/products
    @Operation(summary = "관리자 상품 목록 조회",
            description = "활성여부, 쿠팡 연동상태 등 관리자 전용 필터를 포함합니다. " +
                    "is_active 미입력 시 전체(활성+비활성) 반환.")
    @GetMapping("/admin/products")
    public ResponseEntity<CommonResponse<AdminProductPageResponse>> getAdminProducts(
            @ModelAttribute AdminProductSearchRequest request) {

        return ResponseEntity.ok(CommonResponse.success(productService.getAdminProducts(request)));
    }

    // POST /admin/products
    @Operation(summary = "상품 엑셀 업로드 등록",
            description = "xlsx 파일을 업로드하여 상품을 일괄 등록합니다. 중복 상품명은 덮어쓰기 처리됩니다.")
    @PostMapping(value = "/admin/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ProductUploadResponse>> uploadProducts(
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.ok(CommonResponse.success(productExcelService.upload(file)));
    }

    // DELETE /admin/products/reset
    // /admin/products/{productId} 보다 먼저 선언해야 "reset"이 경로변수로 잡히지 않음
    @Operation(summary = "전체 초기화",
            description = "상품, 영양정보, 브랜드, 카테고리를 모두 삭제하고 시퀀스(id)를 1로 리셋합니다.")
    @DeleteMapping("/admin/products/reset")
    public ResponseEntity<CommonResponse<Void>> resetAll() {
        productService.resetAll();
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // PATCH /admin/products/{productId}
    @Operation(summary = "상품 수정",
            description = "전달된 필드만 수정합니다. null 필드는 기존 값을 유지합니다.")
    @PatchMapping("/admin/products/{productId}")
    public ResponseEntity<CommonResponse<Void>> updateProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @RequestBody AdminProductUpdateRequest request) {

        productService.updateProduct(productId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // DELETE /admin/products/{productId}
    @Operation(summary = "상품 비활성화",
            description = "특정 상품을 소프트 딜리트(is_active=false) 처리합니다. 이미 비활성화된 상품은 409 반환.")
    @DeleteMapping("/admin/products/{productId}")
    public ResponseEntity<CommonResponse<Void>> deactivateProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {

        productService.deactivateProduct(productId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}