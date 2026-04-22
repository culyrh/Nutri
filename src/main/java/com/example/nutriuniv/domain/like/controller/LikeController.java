package com.example.nutriuniv.domain.like.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.like.dto.LikePageResponse;
import com.example.nutriuniv.domain.like.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like", description = "찜 API")
@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // GET /likes
    @Operation(summary = "찜 목록 조회",
            description = "로그인한 사용자의 찜 목록을 최신순으로 반환합니다. 비활성 상품은 제외됩니다.")
    @GetMapping("/likes")
    public ResponseEntity<CommonResponse<LikePageResponse>> getLikes(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(CommonResponse.success(
                likeService.getLikes(principal.getId(), page, size)));
    }

    // POST /likes/{productId}
    @Operation(summary = "찜 추가",
            description = "상품을 찜 목록에 추가합니다. 이미 찜한 상품이면 409를 반환합니다.")
    @PostMapping("/likes/{productId}")
    public ResponseEntity<CommonResponse<Void>> addLike(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal) {

        likeService.addLike(productId, principal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }

    // DELETE /likes/{productId}
    @Operation(summary = "찜 삭제",
            description = "찜 목록에서 상품을 제거합니다.")
    @DeleteMapping("/likes/{productId}")
    public ResponseEntity<CommonResponse<Void>> removeLike(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal) {

        likeService.removeLike(productId, principal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}