package com.example.nutriuniv.domain.category.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.category.dto.CategoryRequest;
import com.example.nutriuniv.domain.category.dto.CategoryTreeResponse;
import com.example.nutriuniv.domain.category.dto.CategoryUpdateRequest;
import com.example.nutriuniv.domain.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // GET /categories
    @Operation(summary = "카테고리 목록 조회", description = "활성 카테고리를 트리 구조로 반환합니다.")
    @GetMapping("/categories")
    public ResponseEntity<CommonResponse<List<CategoryTreeResponse>>> getCategories() {
        return ResponseEntity.ok(CommonResponse.success(categoryService.getCategoryTree()));
    }

    // GET /categories/{categoryId}/brands
    @Operation(summary = "카테고리별 브랜드 목록", description = "특정 카테고리에 속한 브랜드 목록과 상품 수를 반환합니다.")
    @GetMapping("/categories/{categoryId}/brands")
    public ResponseEntity<CommonResponse<List<Map<String, Object>>>> getBrandsByCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId) {
        return ResponseEntity.ok(CommonResponse.success(categoryService.getBrandsByCategory(categoryId)));
    }

    // POST /admin/categories
    @Operation(summary = "카테고리 등록", description = "새 카테고리를 등록합니다. depth=1이면 parentId 불필요.")
    @PostMapping("/admin/categories")
    public ResponseEntity<CommonResponse<Long>> createCategory(
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(CommonResponse.success(categoryService.createCategory(request)));
    }

    // PATCH /admin/categories/{categoryId}
    @Operation(summary = "카테고리 수정", description = "카테고리명, 정렬순서, 활성여부를 수정합니다. null 필드는 기존 값 유지.")
    @PatchMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CommonResponse<Void>> updateCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId,
            @RequestBody CategoryUpdateRequest request) {
        categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // DELETE /admin/categories/{categoryId}
    @Operation(summary = "카테고리 삭제",
            description = "카테고리를 소프트 딜리트(is_active=false) 처리합니다. 하위 카테고리 또는 상품이 존재하면 400 반환.")
    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CommonResponse<Void>> deleteCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}