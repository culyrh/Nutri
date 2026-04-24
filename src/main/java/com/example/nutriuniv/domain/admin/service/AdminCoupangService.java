package com.example.nutriuniv.domain.admin.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
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
                    log.warn("[CoupangSync] 상품명 포함 결과 없음 - productId: {}, keyword: {}", productId, product.getName());
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
}
