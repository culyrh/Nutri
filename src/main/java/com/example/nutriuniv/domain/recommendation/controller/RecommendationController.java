package com.example.nutriuniv.domain.recommendation.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.recommendation.dto.RecommendationResponse;
import com.example.nutriuniv.domain.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation", description = "추천 API")
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "맞춤 상품 추천",
            description = """
                    로그인 유저 기준 3단계 폴백으로 추천 결과를 반환합니다.

                    - CF      : 행동 로그 기반 협업 필터링 결과 (ALS 학습 캐시)
                    - CONTENT : diet_purpose 기반 영양소 벡터 유사도 (콜드스타트)
                    - POPULAR : 전체 조회수 기준 인기 상품 (diet_purpose 없을 때)

                    응답의 type 필드로 어떤 방식으로 추천됐는지 확인할 수 있습니다.
                    """
    )
    @GetMapping
    public ResponseEntity<CommonResponse<RecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal) {

        Long userId = principal.getId();
        RecommendationResponse response = recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
