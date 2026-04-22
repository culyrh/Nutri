package com.example.nutriuniv.domain.search.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.search.dto.PopularKeywordResponse;
import com.example.nutriuniv.domain.search.dto.RecentKeywordResponse;
import com.example.nutriuniv.domain.search.repository.PopularKeywordRepository;
import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PopularKeywordRepository popularKeywordRepository;
    private final SearchLogRepository searchLogRepository;

    // ── GET /search/keywords/popular ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PopularKeywordResponse> getPopularKeywords() {
        return popularKeywordRepository.findTop10ByOrderByRankAsc().stream()
                .map(pk -> PopularKeywordResponse.builder()
                        .rank(pk.getRank())
                        .keyword(pk.getKeyword())
                        .build())
                .collect(Collectors.toList());
    }

    // ── GET /search/keywords/recent ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecentKeywordResponse> getRecentKeywords(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return searchLogRepository.findRecentKeywords(userId, PageRequest.of(0, 10));
    }
}
