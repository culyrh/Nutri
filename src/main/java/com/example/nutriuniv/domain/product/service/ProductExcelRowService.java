package com.example.nutriuniv.domain.product.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.brand.repository.BrandRepository;
import com.example.nutriuniv.domain.category.entity.Category;
import com.example.nutriuniv.domain.category.repository.CategoryRepository;
import com.example.nutriuniv.domain.coupang.client.CoupangApiClient;
import com.example.nutriuniv.domain.coupang.dto.CoupangProductData;
import com.example.nutriuniv.domain.coupang.dto.CoupangSearchResponse;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.coupang.repository.CoupangLinkRepository;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import com.example.nutriuniv.domain.product.repository.ProductNutrientRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelRowService {

    private final ProductRepository productRepository;
    private final ProductNutrientRepository productNutrientRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CoupangApiClient coupangApiClient;
    private final CoupangLinkRepository coupangLinkRepository;

    /**
     * 행 하나를 독립 트랜잭션으로 처리.
     * 실패해도 다른 행 트랜잭션에 영향 없음.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRow(Map<String, String> values,
                           Map<String, Brand> brandCache,
                           Map<String, Category> catCache) {

        String productName = values.getOrDefault("name", "").trim();
        String brandName   = values.getOrDefault("brand", "").trim();
        String depth1Name  = values.getOrDefault("categoryDepth1", "").trim();
        String depth2Name  = values.getOrDefault("categoryDepth2", "").trim();

        if (productName.isEmpty()) throw new CustomException(ErrorCode.BAD_REQUEST, "제품명이 비어있습니다.");
        if (brandName.isEmpty())   throw new CustomException(ErrorCode.BAD_REQUEST, "브랜드가 비어있습니다.");
        if (depth1Name.isEmpty())  throw new CustomException(ErrorCode.BAD_REQUEST, "대분류가 비어있습니다.");
        if (depth2Name.isEmpty())  throw new CustomException(ErrorCode.BAD_REQUEST, "중분류가 비어있습니다.");

        Brand brand = brandCache.computeIfAbsent(brandName, n ->
                brandRepository.findByName(n).orElseGet(() -> brandRepository.save(Brand.create(n))));

        Category depth1 = catCache.computeIfAbsent(depth1Name, n ->
                categoryRepository.findByNameAndDepth(n, 1)
                        .orElseGet(() -> categoryRepository.save(Category.createDepth1(n))));

        String depth2Key = depth1Name + "::" + depth2Name;
        Category depth2 = catCache.computeIfAbsent(depth2Key, k ->
                categoryRepository.findByNameAndDepthAndParent(depth2Name, 2, depth1)
                        .orElseGet(() -> categoryRepository.save(Category.createDepth2(depth2Name, depth1))));

        Product product = productRepository.findByName(productName)
                .orElseGet(() -> productRepository.save(Product.create(productName, depth2, brand)));
        product.update(depth2, brand);

        ProductNutrient nutrient = productNutrientRepository.findById(product.getId())
                .orElseGet(() -> ProductNutrient.create(product));

        nutrient.update(
                values.getOrDefault("servingSize", ""),
                parseNutrient(values.get("calories")),
                parseNutrient(values.get("carbohydrate")),
                parseNutrient(values.get("sugar")),
                parseNutrient(values.get("protein")),
                parseNutrient(values.get("fat")),
                parseNutrient(values.get("saturatedFat")),
                parseNutrient(values.get("transFat")),
                parseNutrient(values.get("cholesterol")),
                parseNutrient(values.get("sodium")),
                parseNutrient(values.get("fiber"))
        );
        productNutrientRepository.save(nutrient);

        mapCoupangLink(product, productName);
    }

    private void mapCoupangLink(Product product, String keyword) {
        CoupangLink link = coupangLinkRepository.findByProduct(product)
                .orElseGet(() -> coupangLinkRepository.save(CoupangLink.createDefault(product)));
        try {
            CoupangSearchResponse.SearchData searchData = coupangApiClient.searchProduct(keyword);
            if (searchData == null) {
                link.syncFailed();
                return;
            }
            String normalizedKeyword = normalize(keyword);
            CoupangProductData data = searchData.getProductData().stream()
                    .filter(p -> p.getProductName() != null
                            && normalize(p.getProductName()).contains(normalizedKeyword))
                    .findFirst()
                    .orElse(null);

            if (data == null) {
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
                product.updateImageUrl(data.getProductImage());
            }
        } catch (Exception e) {
            log.warn("[ExcelUpload] 쿠팡 매핑 실패 - keyword: {}, error: {}", keyword, e.getMessage());
            link.syncFailed();
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    private BigDecimal parseNutrient(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        String numOnly = raw.replaceAll("[^0-9.]", "").trim();
        if (numOnly.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(numOnly);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
