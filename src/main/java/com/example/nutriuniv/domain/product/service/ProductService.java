package com.example.nutriuniv.domain.product.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.domain.brand.entity.Brand;
import com.example.nutriuniv.domain.brand.repository.BrandRepository;
import com.example.nutriuniv.domain.category.entity.Category;
import com.example.nutriuniv.domain.category.repository.CategoryRepository;
import com.example.nutriuniv.domain.like.repository.UserFavoriteRepository;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.coupang.repository.CoupangLinkRepository;
import com.example.nutriuniv.domain.pns.service.PnsLookupService;
import com.example.nutriuniv.domain.product.dto.*;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import com.example.nutriuniv.domain.product.repository.ProductNutrientRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import com.example.nutriuniv.domain.product.specification.ProductSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> ALLOWED_SORT_VALUES = Set.of("POPULAR", "SCORE", "ACCURACY", "RECOMMENDED");
    private static final Set<String> ALLOWED_LINK_STATUS = Set.of("LINKED", "UNLINKED", "FAILED");

    private final ProductRepository productRepository;
    private final ProductNutrientRepository productNutrientRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final EntityManager entityManager;
    private final CoupangLinkRepository coupangLinkRepository;
    private final PnsLookupService pnsLookupService;

    // ── 일반 유저: 상품 목록 조회 ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(ProductSearchRequest request, Long userId) {

        if (request.getPage() < 0 || request.getSize() <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }
        if (request.getSort() != null && !ALLOWED_SORT_VALUES.contains(request.getSort())) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }
        validateMinMax(request);

        Specification<Product> spec = Specification
                .where(ProductSpecification.isActive())
                .and(ProductSpecification.hasKeyword(request.getKeyword()))
                .and(ProductSpecification.hasCategory(request.getCategoryId()))
                .and(ProductSpecification.hasBrand(request.getBrandId()))
                .and(ProductSpecification.hasMinCalories(request.getMinCalories()))
                .and(ProductSpecification.hasMaxCalories(request.getMaxCalories()))
                .and(ProductSpecification.hasMinProtein(request.getMinProtein()))
                .and(ProductSpecification.hasMaxProtein(request.getMaxProtein()))
                .and(ProductSpecification.hasMinFat(request.getMinFat()))
                .and(ProductSpecification.hasMaxFat(request.getMaxFat()))
                .and(ProductSpecification.hasMinCarbohydrate(request.getMinCarbohydrate()))
                .and(ProductSpecification.hasMaxCarbohydrate(request.getMaxCarbohydrate()))
                .and(ProductSpecification.hasMinSugar(request.getMinSugar()))
                .and(ProductSpecification.hasMaxSugar(request.getMaxSugar()))
                .and(ProductSpecification.hasMinSodium(request.getMinSodium()))
                .and(ProductSpecification.hasMaxSodium(request.getMaxSodium()))
                .and(ProductSpecification.hasMinNutritionScore(request.getMinNutritionScore()))
                .and(ProductSpecification.hasMaxNutritionScore(request.getMaxNutritionScore()));

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), resolveSort(request.getSort()));
        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductListResponse> items = page.getContent().stream()
                .map(p -> toListResponse(p, userId))
                .collect(Collectors.toList());

        return ProductPageResponse.builder()
                .total(page.getTotalElements())
                .page(request.getPage())
                .size(request.getSize())
                .items(items)
                .build();
    }

    // ── 일반 유저: 상품 상세 조회 ─────────────────────────────────────────────────

    @Transactional
    public ProductDetailResponse getProduct(Long productId, Long userId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!product.isActive()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        product.increaseViewCount();

        ProductNutrient nutrient = productNutrientRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        CoupangLink coupangLink = coupangLinkRepository.findByProduct(product).orElse(null);

        // PNS — 사용자 EER 구간 결정 후 점수/등급/백분위 + 대분류 총 개수 조회
        int eerBand = pnsLookupService.resolveEerBand(userId);
        PnsLookupService.PnsLookupResult pnsResult = pnsLookupService.lookup(product.getId(), eerBand);
        ProductDetailResponse.PnsInfo pnsInfo = buildPnsInfo(product, pnsResult, eerBand);

        return toDetailResponse(product, nutrient, userId, coupangLink, pnsInfo);
    }

    private ProductDetailResponse.PnsInfo buildPnsInfo(Product product,
                                                        PnsLookupService.PnsLookupResult result,
                                                        int eerBand) {
        if (result == null) return null;

        Category parent = product.getCategory().getParent();
        Long parentId   = parent == null ? null : parent.getId();
        String parentName = parent == null ? null : parent.getName();
        int total = pnsLookupService.countActiveByParentCategory(parentId);

        return ProductDetailResponse.PnsInfo.builder()
                .score(result.score())
                .grade(result.grade())
                .percentile(result.percentile())
                .topPercent(result.topPercent())
                .parentCategoryId(parentId)
                .parentCategoryName(parentName)
                .categoryTotal(total)
                .eerBand(eerBand)
                .build();
    }

    // ── 관리자: 상품 목록 조회 ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminProductPageResponse getAdminProducts(AdminProductSearchRequest request) {

        if (request.getPage() < 0 || request.getSize() <= 0) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }
        if (request.getLinkStatus() != null && !ALLOWED_LINK_STATUS.contains(request.getLinkStatus())) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }

        Specification<Product> spec = Specification
                .where(ProductSpecification.isActiveAdmin(request.getIsActive()))
                .and(ProductSpecification.hasKeyword(request.getKeyword()))
                .and(ProductSpecification.hasCategory(request.getCategoryId()))
                .and(ProductSpecification.hasBrand(request.getBrandId()))
                .and(ProductSpecification.hasLinkStatus(request.getLinkStatus()));
        // linkStatus 필터는 CoupangLink 엔티티 구현 후 추가 -> 추가 완료

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> page = productRepository.findAll(spec, pageable);

        List<AdminProductPageResponse.AdminProductListResponse> items = page.getContent().stream()
                .map(this::toAdminListResponse)
                .collect(Collectors.toList());

        return AdminProductPageResponse.builder()
                .total(page.getTotalElements())
                .page(request.getPage())
                .size(request.getSize())
                .items(items)
                .build();
    }

    // ── 관리자: 상품 수정 ─────────────────────────────────────────────────────────

    @Transactional
    public void updateProduct(Long productId, AdminProductUpdateRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        Category category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 카테고리입니다."))
                : product.getCategory();

        Brand brand = request.getBrandId() != null
                ? brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 브랜드입니다."))
                : product.getBrand();

        String name          = request.getName()           != null ? request.getName()           : product.getName();
        String imageUrl      = request.getImageUrl()       != null ? request.getImageUrl()       : product.getImageUrl();
        BigDecimal score     = request.getNutritionScore() != null ? request.getNutritionScore() : product.getNutritionScore();

        product.update(name, category, brand, imageUrl, score);

        if (request.getIsActive() != null) {
            if (request.getIsActive()) product.activate();
            else product.deactivate();
        }
    }

    // ── 관리자: 상품 비활성화 ─────────────────────────────────────────────────────

    @Transactional
    public void deactivateProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!product.isActive()) {
            throw new CustomException(ErrorCode.STATE_CONFLICT, "이미 비활성화된 상품입니다.");
        }

        product.deactivate();
    }

    // ── 관리자: 전체 초기화 ───────────────────────────────────────────────────────
    // 현재 구현된 테이블만 포함. 추후 도메인 추가 시 해당 테이블도 여기에 추가할 것.
    //
    // 아래 테이블 구현 후 TRUNCATE 목록에 추가
    //   - coupang_links       (쿠팡 연동 구현 시) -> 추가 완료
    //   - user_favorites      (찜 도메인 구현 시) -> 추가 완료
    //   TODO: - user_compares       (비교 도메인 구현 시)
    //   - reviews, review_images (리뷰 도메인 구현 시) -> 추가 완료
    //   - product_view_logs   (로깅 도메인 구현 시) -> 추가 완료

    @Transactional
    public void resetAll() {

        // 1. user_favorites (products FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE user_favorites RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 2. review_images (reviews FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE review_images RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 3. reviews (products, users FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE reviews RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 4. coupang_links (products FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE coupang_links RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 5. product_view_logs (products FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE product_view_logs RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 6. product_nutrients (products FK 참조)
        entityManager.createNativeQuery(
                "TRUNCATE TABLE product_nutrients RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 7. products 본체
        entityManager.createNativeQuery(
                "TRUNCATE TABLE products RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 8. brands
        entityManager.createNativeQuery(
                "TRUNCATE TABLE brands RESTART IDENTITY CASCADE"
        ).executeUpdate();

        // 9. categories: 셀프 참조(parent_id) 때문에 depth 역순으로 DELETE 후 시퀀스 리셋
        entityManager.createNativeQuery(
                "DELETE FROM categories WHERE depth = 3"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM categories WHERE depth = 2"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM categories WHERE depth = 1"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "ALTER SEQUENCE categories_id_seq RESTART WITH 1"
        ).executeUpdate();

//        // 10. search_logs (일단 추가는 함)
//        entityManager.createNativeQuery(
//                "TRUNCATE TABLE search_logs RESTART IDENTITY CASCADE"
//        ).executeUpdate();
    }

    // ── 검증 헬퍼 ─────────────────────────────────────────────────────────────────

    private void validateMinMax(ProductSearchRequest req) {
        if (isInvalid(req.getMinCalories(),      req.getMaxCalories())      ||
                isInvalid(req.getMinProtein(),        req.getMaxProtein())       ||
                isInvalid(req.getMinFat(),            req.getMaxFat())           ||
                isInvalid(req.getMinCarbohydrate(),   req.getMaxCarbohydrate())  ||
                isInvalid(req.getMinSugar(),          req.getMaxSugar())         ||
                isInvalid(req.getMinSodium(),         req.getMaxSodium())        ||
                isInvalid(req.getMinNutritionScore(), req.getMaxNutritionScore())) {
            throw new CustomException(ErrorCode.INVALID_QUERY_PARAM);
        }
    }

    private boolean isInvalid(BigDecimal min, BigDecimal max) {
        return min != null && max != null && min.compareTo(max) > 0;
    }

    // ── 정렬 변환 ─────────────────────────────────────────────────────────────────

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case "POPULAR"     -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "SCORE"       -> Sort.by(Sort.Direction.DESC, "nutritionScore");
            case "ACCURACY",
                 "RECOMMENDED" -> Sort.by(Sort.Direction.DESC, "createdAt");   // TODO: 추후 구현
            default            -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // ── 변환 메서드 ───────────────────────────────────────────────────────────────

    private ProductListResponse toListResponse(Product product, Long userId) {
        boolean favorited = userId != null &&
                userFavoriteRepository.existsByUserIdAndProductIdAndProductIsActiveTrue(userId, product.getId());

        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .nutritionScore(product.getNutritionScore())
                .isFavorited(favorited)
                .brand(product.getBrand() == null ? null : ProductListResponse.BrandInfo.builder()
                        .id(product.getBrand().getId())
                        .name(product.getBrand().getName())
                        .build())
                .category(ProductListResponse.CategoryInfo.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .build();
    }

    private ProductDetailResponse toDetailResponse(Product product, ProductNutrient nutrient,
                                                   Long userId, CoupangLink coupangLink,
                                                   ProductDetailResponse.PnsInfo pnsInfo) {
        boolean favorited = userId != null &&
                userFavoriteRepository.existsByUserIdAndProductIdAndProductIsActiveTrue(userId, product.getId());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .nutritionScore(product.getNutritionScore())
                .viewCount(product.getViewCount())
                .isFavorited(favorited)  // like 도메인 구현 후 채울 예정 -> 추가 완료
                .scoreRankPercent(null)   // TODO: 추후 구현
                .brand(product.getBrand() == null ? null : ProductDetailResponse.BrandInfo.builder()
                        .id(product.getBrand().getId())
                        .name(product.getBrand().getName())
                        .build())
                .category(ProductDetailResponse.CategoryInfo.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .nutrients(ProductDetailResponse.NutrientInfo.builder()
                        .servingSize(nutrient.getServingSize())
                        .calories(nutrient.getCalories())
                        .carbohydrate(nutrient.getCarbohydrate())
                        .sugar(nutrient.getSugar())
                        .protein(nutrient.getProtein())
                        .fat(nutrient.getFat())
                        .saturatedFat(nutrient.getSaturatedFat())
                        .transFat(nutrient.getTransFat())
                        .cholesterol(nutrient.getCholesterol())
                        .sodium(nutrient.getSodium())
                        .build())
                .coupang(coupangLink == null || !"LINKED".equals(coupangLink.getLinkStatus())
                        ? null
                        : ProductDetailResponse.CoupangInfo.builder()
                        .affiliateUrl(coupangLink.getAffiliateUrl())
                        .landingUrl(coupangLink.getLandingUrl())
                        .price(coupangLink.getProductPrice())
                        .isRocket(coupangLink.getIsRocket())
                        .isFreeShipping(coupangLink.getIsFreeShipping())
                        .lastSyncedAt(coupangLink.getLastSyncedAt())
                        .build())   // coupang 도메인 구현 후 채울 예정 -> 추가 완료
                .pns(pnsInfo)
                .build();
    }

    private AdminProductPageResponse.AdminProductListResponse toAdminListResponse(Product product) {
        return AdminProductPageResponse.AdminProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .isActive(product.isActive())
                .nutritionScore(product.getNutritionScore())
                .linkStatus(product.getCoupangLink() != null
                        ? product.getCoupangLink().getLinkStatus()
                        : "UNLINKED")  // CoupangLink 구현 후 채울 예정 -> 추가 완료
                .brand(product.getBrand() == null ? null : AdminProductPageResponse.BrandInfo.builder()
                        .id(product.getBrand().getId())
                        .name(product.getBrand().getName())
                        .build())
                .category(AdminProductPageResponse.CategoryInfo.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .build();
    }
}