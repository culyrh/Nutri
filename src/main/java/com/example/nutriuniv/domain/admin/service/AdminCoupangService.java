package com.example.nutriuniv.domain.admin.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangBulkSyncResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangLinkPageResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangLinkResponse;
import com.example.nutriuniv.domain.admin.dto.AdminCoupangSyncResponse;
import com.example.nutriuniv.domain.coupang.client.CoupangApiClient;
import com.example.nutriuniv.domain.coupang.dto.CoupangProductData;
import com.example.nutriuniv.domain.coupang.dto.CoupangSearchResponse;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.coupang.repository.CoupangLinkRepository;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoupangService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("LINKED", "UNLINKED", "FAILED");

    private final CoupangLinkRepository coupangLinkRepository;
    private final ProductRepository productRepository;
    private final CoupangApiClient coupangApiClient;

    // ── GET /admin/coupang/links ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminCoupangLinkPageResponse getCoupangLinks(String status, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM, "page는 0 이상, size는 1 이상이어야 합니다.");
        }
        if (status != null && !ALLOWED_STATUSES.contains(status.toUpperCase())) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM, "허용되지 않는 status 값입니다. (LINKED / UNLINKED / FAILED)");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<CoupangLink> linkPage = (status == null)
                ? coupangLinkRepository.findAll(pageable)
                : coupangLinkRepository.findByLinkStatus(status.toUpperCase(), pageable);

        List<AdminCoupangLinkResponse> items = linkPage.getContent().stream()
                .map(AdminCoupangLinkResponse::from)
                .collect(Collectors.toList());

        return AdminCoupangLinkPageResponse.builder()
                .total(linkPage.getTotalElements())
                .items(items)
                .build();
    }

    // ── POST /admin/coupang/sync/{productId} ──────────────────────────────────────

    @Transactional
    public AdminCoupangSyncResponse syncCoupangLink(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 상품입니다."));

        CoupangLink link = coupangLinkRepository.findByProduct(product)
                .orElseGet(() -> coupangLinkRepository.save(CoupangLink.createDefault(product)));

        try {
            CoupangSearchResponse.SearchData searchData = coupangApiClient.searchProduct(product.getName());

            if (searchData == null) {
                log.warn("[CoupangSync] 검색 결과 없음 - productId: {}, keyword: {}", productId, product.getName());
                link.syncFailed();
            } else {
                CoupangProductData data = searchData.getProductData().stream()
                        .filter(p -> p.getProductName() != null &&
                                     p.getProductName().contains(product.getName()))
                        .findFirst()
                        .orElse(null);

                if (data == null) {
                    List<String> candidates = searchData.getProductData().stream()
                            .map(CoupangProductData::getProductName)
                            .collect(Collectors.toList());
                    log.warn("[CoupangSync] 상품명 불일치 - productId: {}, keyword: {}, 쿠팡 반환 목록: {}",
                            productId, product.getName(), candidates);
                    link.syncFailed();
                } else {
                    link.syncSuccess(
                            String.valueOf(data.getProductId()),
                            data.getProductName(),
                            data.getProductUrl(),
                            searchData.getLandingUrl(),
                            data.getProductImage(),
                            data.getProductPrice(),
                            data.getIsRocket(),
                            data.getIsFreeShipping()
                    );
                }
            }
        } catch (Exception e) {
            log.error("[CoupangSync] 쿠팡 API 연동 실패 - productId: {}, error: {}", productId, e.getMessage());
            link.syncFailed();
        }

        return AdminCoupangSyncResponse.from(link);
    }

    // ── POST /admin/coupang/sync ──────────────────────────────────────────────────

    public AdminCoupangBulkSyncResponse bulkSyncCoupangLinks() {
        List<CoupangLink> targets = coupangLinkRepository.findAllByLinkStatus("UNLINKED");
        log.info("[CoupangBulkSync] UNLINKED 매핑 대상 수: {}", targets.size());
        return processBulk(targets, "CoupangBulkSync");
    }

    // ── POST /admin/coupang/retry ─────────────────────────────────────────────────

    public AdminCoupangBulkSyncResponse retryCoupangLinks() {
        List<CoupangLink> targets = coupangLinkRepository.findAllByLinkStatus("FAILED");
        log.info("[CoupangRetry] ============================================================");
        log.info("[CoupangRetry] FAILED 재시도 시작 — 대상: {}건, 호출 간격: 3초, 점수 임계값: {}",
                targets.size(), SCORE_THRESHOLD);
        log.info("[CoupangRetry] ============================================================");
        return processBulkWithScoring(targets, "CoupangRetry");
    }

    // ── 공통 매핑 처리 ─────────────────────────────────────────────────────────────

    private AdminCoupangBulkSyncResponse processBulk(List<CoupangLink> targets, String logTag) {
        int successCount = 0;
        int failCount    = 0;

        for (CoupangLink link : targets) {
            try {
                Thread.sleep(3000); // 분당 30회 → 제한(50회) 대비 여유있게
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            try {
                Product product = link.getProduct();
                CoupangSearchResponse.SearchData searchData = coupangApiClient.searchProduct(product.getName());

                if (searchData == null) {
                    log.warn("[{}] 검색 결과 없음 - productId: {}, keyword: {}", logTag, product.getId(), product.getName());
                    link.syncFailed();
                    failCount++;
                } else {
                    CoupangProductData data = searchData.getProductData().stream()
                            .filter(p -> p.getProductName() != null &&
                                         p.getProductName().contains(product.getName()))
                            .findFirst()
                            .orElse(null);

                    if (data == null) {
                        List<String> candidates = searchData.getProductData().stream()
                                .map(CoupangProductData::getProductName)
                                .collect(Collectors.toList());
                        log.warn("[{}] 상품명 불일치 - productId: {}, keyword: {}, 쿠팡 반환 목록: {}",
                                logTag, product.getId(), product.getName(), candidates);
                        link.syncFailed();
                        failCount++;
                    } else {
                        link.syncSuccess(
                                String.valueOf(data.getProductId()),
                                data.getProductName(),
                                data.getProductUrl(),
                                searchData.getLandingUrl(),
                                data.getProductImage(),
                                data.getProductPrice(),
                                data.getIsRocket(),
                                data.getIsFreeShipping()
                        );
                        product.updateImageUrl(data.getProductImage());
                        successCount++;
                        log.info("[{}] 매핑 성공 - productId: {}, name: {}", logTag, product.getId(), product.getName());
                    }
                }
                coupangLinkRepository.save(link);

            } catch (Exception e) {
                log.error("[{}] 예외 발생 - productId: {}, error: {}", logTag, link.getProduct().getId(), e.getMessage());
                link.syncFailed();
                failCount++;
                coupangLinkRepository.save(link);
            }
        }

        log.info("[{}] 완료 - 성공: {}, 실패: {}", logTag, successCount, failCount);
        return AdminCoupangBulkSyncResponse.builder()
                .totalCount(targets.size())
                .successCount(successCount)
                .failCount(failCount)
                .build();
    }

    // ── 점수 기반 매칭 처리 (retry 전용) ─────────────────────────────────────────
    //
    // 한 상품당 최대 3차까지 키워드를 단축해가며 검색한다.
    //   1차: 원본 키워드          (예: "꼬기다 닭가슴살 갈릭크림맛")
    //   2차: 마지막 토큰 제거     (예: "꼬기다 닭가슴살")   ※ 토큰 ≥3 일 때만
    //   3차: 첫 토큰만 (브랜드)   (예: "꼬기다")           ※ 토큰 ≥2 일 때만
    //
    // 각 차수의 후보 10개에 대해 점수 규칙 적용. 점수는 항상 **원본 키워드** 기준.
    //   100 - 정규화(공백/괄호/특수문자 제거, 소문자) 후 키워드 전체 포함
    //    80 - 모든 토큰이 후보명에 포함됨
    //    70 - 정규화 키워드의 2-gram 75% 이상이 후보에 포함 (들깨감자탕↔들깨순살감자탕 같은 합성어 케이스)
    //    60 - 토큰 75% 이상 일치 (토큰 ≥ 2개일 때, 예: 4토큰 중 3개)
    //    40 - 토큰 50% 이상 일치 (토큰 ≥ 2개일 때)
    //     0 - 매칭 없음
    //
    // 어느 차수에서든 임계값(SCORE_THRESHOLD)을 넘으면 즉시 채택하고 다음 상품으로.
    // 모든 차수 실패 시 FAILED.
    //
    // 모든 쿠팡 호출 직전에 3초 sleep (throttledSearch 헬퍼) → 분당 50회 제한 대비.

    private static final int SCORE_THRESHOLD = 60;

    private record ScoredCandidate(CoupangProductData data, int score, String rule) {}

    private AdminCoupangBulkSyncResponse processBulkWithScoring(List<CoupangLink> targets, String logTag) {
        int successCount = 0;
        int failCount    = 0;
        int total        = targets.size();
        int idx          = 0;

        outer:
        for (CoupangLink link : targets) {
            idx++;
            Product product       = link.getProduct();
            String originalKeyword = product.getName();
            List<String> variants  = buildKeywordVariants(originalKeyword);

            log.info("[{}] ({}/{}) productId={}, keyword='{}', 변형 {}단계",
                    logTag, idx, total, product.getId(), originalKeyword, variants.size());

            try {
                boolean matched               = false;
                ScoredCandidate bestSoFar     = new ScoredCandidate(null, 0, "none");
                List<CoupangProductData> lastCandidates = List.of();

                for (int pass = 0; pass < variants.size(); pass++) {
                    String kw    = variants.get(pass);
                    int passNum  = pass + 1;

                    log.info("[{}]     {}차 시도 — keyword='{}'", logTag, passNum, kw);

                    CoupangSearchResponse.SearchData searchData;
                    try {
                        searchData = throttledSearch(kw);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("[{}] !!! 사용자 중단 감지 — 처리 중단 (진행: {}/{}, 성공: {}, 실패: {})",
                                logTag, idx - 1, total, successCount, failCount);
                        break outer;
                    }

                    if (searchData == null
                            || searchData.getProductData() == null
                            || searchData.getProductData().isEmpty()) {
                        log.warn("[{}]     {}차 — 검색 결과 0건", logTag, passNum);
                        continue;
                    }

                    List<CoupangProductData> candidates = searchData.getProductData();
                    lastCandidates = candidates;

                    // 점수는 항상 원본 키워드 기준으로 계산
                    ScoredCandidate best = pickBest(candidates, originalKeyword);
                    log.info("[{}]     {}차 — 최고점={}/100 (rule={})",
                            logTag, passNum, best.score(), best.rule());

                    if (best.score() >= SCORE_THRESHOLD) {
                        CoupangProductData data = best.data();
                        link.syncSuccess(
                                String.valueOf(data.getProductId()),
                                data.getProductName(),
                                data.getProductUrl(),
                                searchData.getLandingUrl(),
                                data.getProductImage(),
                                data.getProductPrice(),
                                data.getIsRocket(),
                                data.getIsFreeShipping()
                        );
                        product.updateImageUrl(data.getProductImage());
                        successCount++;
                        matched = true;
                        log.info("[{}]     OK 매칭 — score={}/100, rule={}, 사용키워드='{}', 채택='{}'",
                                logTag, best.score(), best.rule(), kw, data.getProductName());
                        break;
                    }

                    if (best.score() > bestSoFar.score()) {
                        bestSoFar = best;
                    }
                }

                if (!matched) {
                    link.syncFailed();
                    failCount++;
                    log.warn("[{}] >>> 모든 차수 실패 — productId={}, keyword='{}', 누적최고점={}/100 (rule={})",
                            logTag, product.getId(), originalKeyword, bestSoFar.score(), bestSoFar.rule());
                    int shown = 0;
                    for (CoupangProductData c : lastCandidates) {
                        if (shown++ >= 3) break;
                        log.warn("[{}]       후보 {}: '{}'", logTag, shown, c.getProductName());
                    }
                }
                coupangLinkRepository.save(link);

            } catch (Exception e) {
                log.error("[{}] !!! 예외 발생 — productId={}, error={}",
                        logTag, product.getId(), e.getMessage(), e);
                link.syncFailed();
                failCount++;
                coupangLinkRepository.save(link);
            }
        }

        log.info("[{}] ============================================================", logTag);
        log.info("[{}] 완료 — 전체: {}, 성공: {}, 실패: {}",
                logTag, total, successCount, failCount);
        log.info("[{}] ============================================================", logTag);

        return AdminCoupangBulkSyncResponse.builder()
                .totalCount(total)
                .successCount(successCount)
                .failCount(failCount)
                .build();
    }

    /** 모든 쿠팡 호출 직전에 3초 슬립 — 분당 50회 제한 대비. */
    private CoupangSearchResponse.SearchData throttledSearch(String keyword) throws InterruptedException {
        Thread.sleep(3000);
        return coupangApiClient.searchProduct(keyword);
    }

    /** 키워드 단축 변형: 원본 → (토큰≥3) 마지막 토큰 제거 → (토큰≥2) 브랜드(첫 토큰)만. */
    private List<String> buildKeywordVariants(String original) {
        List<String> tokens   = tokenize(original);
        List<String> variants = new ArrayList<>();
        variants.add(original);
        if (tokens.size() >= 3) {
            variants.add(String.join(" ", tokens.subList(0, tokens.size() - 1)));
        }
        if (tokens.size() >= 2) {
            variants.add(tokens.get(0));
        }
        return variants;
    }

    private ScoredCandidate pickBest(List<CoupangProductData> candidates, String keyword) {
        ScoredCandidate best = new ScoredCandidate(null, 0, "none");
        for (CoupangProductData c : candidates) {
            if (c.getProductName() == null) continue;
            ScoredCandidate scored = scoreCandidate(c, keyword);
            if (scored.score() > best.score()) {
                best = scored;
            }
        }
        return best;
    }

    private ScoredCandidate scoreCandidate(CoupangProductData data, String keyword) {
        String normalizedProduct = normalize(data.getProductName());
        String normalizedKeyword = normalize(keyword);

        if (!normalizedKeyword.isEmpty() && normalizedProduct.contains(normalizedKeyword)) {
            return new ScoredCandidate(data, 100, "normalized-contains");
        }

        List<String> tokens = tokenize(keyword);
        if (tokens.isEmpty()) {
            return new ScoredCandidate(data, 0, "no-match");
        }

        long matched = tokens.stream()
                .filter(t -> normalizedProduct.contains(normalize(t)))
                .count();

        if (matched == tokens.size()) {
            return new ScoredCandidate(data, 80, "all-tokens");
        }

        // 한국어 합성어/띄어쓰기 변형 케이스용 2-gram 부분 매칭.
        // 정규화 키워드 길이 ≥ 4 일 때만 — 너무 짧으면 우연 매칭 위험.
        if (normalizedKeyword.length() >= 4) {
            double ngramRatio = bigramOverlapRatio(normalizedKeyword, normalizedProduct);
            if (ngramRatio >= 0.75) {
                return new ScoredCandidate(data, 70, "ngram-overlap");
            }
        }

        if (tokens.size() >= 2) {
            double ratio = (double) matched / tokens.size();
            if (ratio >= 0.75) return new ScoredCandidate(data, 60, "most-tokens");
            if (ratio >= 0.5)  return new ScoredCandidate(data, 40, "half-tokens");
        }
        return new ScoredCandidate(data, 0, "no-match");
    }

    /** 정규화된 keyword의 2-gram 슬라이딩 윈도우 중 text에 포함된 비율. */
    private double bigramOverlapRatio(String keyword, String text) {
        int len = keyword.length();
        if (len < 2) return 0.0;
        int total = len - 1;
        int hit   = 0;
        for (int i = 0; i < total; i++) {
            if (text.contains(keyword.substring(i, i + 2))) hit++;
        }
        return (double) hit / total;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[\\s\\[\\]()\\-_,/]", "");
    }

    private List<String> tokenize(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.trim().split("\\s+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }
}
