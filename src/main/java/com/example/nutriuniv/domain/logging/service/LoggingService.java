package com.example.nutriuniv.domain.logging.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.logging.dto.SearchLogRequest;
import com.example.nutriuniv.domain.logging.dto.ViewLogRequest;
import com.example.nutriuniv.domain.logging.entity.ProductViewLog;
import com.example.nutriuniv.domain.logging.entity.SearchLog;
import com.example.nutriuniv.domain.logging.repository.ProductViewLogRepository;
import com.example.nutriuniv.domain.logging.repository.SearchLogRepository;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {

    private final ProductViewLogRepository productViewLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final ProductRepository productRepository;

    // ── POST /logging/view ────────────────────────────────────────────────────────

    @Transactional
    public void logProductView(ViewLogRequest request, Long userId, String ipAddress) {
        if (request.getProductId() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "product_id는 필수입니다.");
        }

        try {
            Product product = productRepository.findById(request.getProductId()).orElse(null);
            if (product == null) {
                log.warn("[ViewLog] 존재하지 않는 product_id: {}", request.getProductId());
                return;
            }
            productViewLogRepository.save(ProductViewLog.create(product, userId, ipAddress));
        } catch (Exception e) {
            log.error("[ViewLog] 로그 저장 실패 - productId: {}, error: {}", request.getProductId(), e.getMessage());
        }
    }

    // ── POST /logging/search ──────────────────────────────────────────────────────

    @Transactional
    public void logSearch(SearchLogRequest request, Long userId) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "keyword는 필수입니다.");
        }

        try {
            searchLogRepository.save(SearchLog.create(userId, request.getKeyword().trim(), request.getResultCount()));
        } catch (Exception e) {
            log.error("[SearchLog] 로그 저장 실패 - keyword: {}, error: {}", request.getKeyword(), e.getMessage());
        }
    }
}
