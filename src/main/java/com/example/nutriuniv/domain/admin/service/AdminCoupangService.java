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
        log.info("[CoupangRetry] FAILED 재시도 대상 수: {}", targets.size());
        return processBulk(targets, "CoupangRetry");
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
}
