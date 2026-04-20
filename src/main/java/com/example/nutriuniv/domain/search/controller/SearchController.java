package com.example.nutriuniv.domain.search.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.domain.search.dto.PopularKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import com.example.nutriuniv.domain.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Search", description = "검색 API")
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // GET /search/keywords/popular
    @Operation(summary = "인기 검색어 조회",
            description = "검색 횟수 기준으로 집계된 인기 검색어를 rank 순으로 최대 10개 반환합니다.")
    @GetMapping("/search/keywords/popular")
    public ResponseEntity<CommonResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
        return ResponseEntity.ok(CommonResponse.success(searchService.getPopularKeywords()));
    }

    // GET /search/keywords/recent
    @Operation(summary = "최근 검색어 조회",
            description = "로그인한 사용자의 최근 검색어를 최대 10개 반환합니다. " +
                    "동일 키워드는 가장 최근 검색 시각으로 중복 제거됩니다. " +
                    "비로그인 시 401을 반환합니다.")
    @GetMapping("/search/keywords/recent")
    public ResponseEntity<CommonResponse<List<RecentKeywordResponse>>> getRecentKeywords() {
        Long userId = null;   // TODO: 인증 구현 후 SecurityContext에서 꺼낼 예정
        return ResponseEntity.ok(CommonResponse.success(searchService.getRecentKeywords(userId)));
    }
}
