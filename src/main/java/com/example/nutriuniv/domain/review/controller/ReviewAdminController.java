package com.example.nutriuniv.domain.review.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.review.service.ReviewAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Review", description = "관리자 리뷰 API")
@RestController
@RequiredArgsConstructor
public class ReviewAdminController {

    private final ReviewAdminService reviewAdminService;

    // DELETE /admin/reviews/{reviewId}
    @Operation(summary = "리뷰 강제 삭제",
            description = "관리자가 특정 리뷰를 소프트 딜리트(is_active=false)합니다.")
    @DeleteMapping("/admin/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<Void>> forceDeleteReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId) {

        reviewAdminService.forceDeleteReview(reviewId);
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}
