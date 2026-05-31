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
import com.example.nutriuniv.domain.product.enums.NutrientClaim;
import com.example.nutriuniv.domain.product.repository.ProductNutrientRepository;
import com.example.nutriuniv.domain.product.repository.ProductRepository;
import com.example.nutriuniv.domain.product.specification.NutrientClaimSpecification;
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

        Specification<Product> spec = Specification.where(ProductSpecification.isActive());
        spec = andIf(spec, request.getKeyword() != null, ProductSpecification.hasKeyword(request.getKeyword()));
        spec = andIf(spec, request.getCategoryIds() != null && !request.getCategoryIds().isEmpty(), ProductSpecification.hasCategory(request.getCategoryIds()));
        spec = andIf(spec, request.getBrandIds() != null && !request.getBrandIds().isEmpty(), ProductSpecification.hasBrand(request.getBrandIds()));
        spec = andIf(spec, request.getMinCalories() != null, ProductSpecification.hasMinCalories(request.getMinCalories()));
        spec = andIf(spec, request.getMaxCalories() != null, ProductSpecification.hasMaxCalories(request.getMaxCalories()));
        spec = andIf(spec, request.getMinProtein() != null, ProductSpecification.hasMinProtein(request.getMinProtein()));
        spec = andIf(spec, request.getMaxProtein() != null, ProductSpecification.hasMaxProtein(request.getMaxProtein()));
        spec = andIf(spec, request.getMinFat() != null, ProductSpecification.hasMinFat(request.getMinFat()));
        spec = andIf(spec, request.getMaxFat() != null, ProductSpecification.hasMaxFat(request.getMaxFat()));
        spec = andIf(spec, request.getMinCarbohydrate() != null, ProductSpecification.hasMinCarbohydrate(request.getMinCarbohydrate()));
        spec = andIf(spec, request.getMaxCarbohydrate() != null, ProductSpecification.hasMaxCarbohydrate(request.getMaxCarbohydrate()));
        spec = andIf(spec, request.getMinSugar() != null, ProductSpecification.hasMinSugar(request.getMinSugar()));
        spec = andIf(spec, request.getMaxSugar() != null, ProductSpecification.hasMaxSugar(request.getMaxSugar()));
        spec = andIf(spec, request.getMinSodium() != null, ProductSpecification.hasMinSodium(request.getMinSodium()));
        spec = andIf(spec, request.getMaxSodium() != null, ProductSpecification.hasMaxSodium(request.getMaxSodium()));
        spec = andIf(spec, request.getMinNutritionScore() != null, ProductSpecification.hasMinNutritionScore(request.getMinNutritionScore()));
        spec = andIf(spec, request.getMaxNutritionScore() != null, ProductSpecification.hasMaxNutritionScore(request.getMaxNutritionScore()));

        List<NutrientClaim> claims = parseClaims(request.getNutrientClaims());
        if (!claims.isEmpty()) {
            spec = spec.and(NutrientClaimSpecification.hasClaims(claims));
        }

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

        Specification<Product> spec = Specification.where(ProductSpecification.isActiveAdmin(request.getIsActive()));
        spec = andIf(spec, request.getKeyword() != null, ProductSpecification.hasKeyword(request.getKeyword()));
        spec = andIf(spec, request.getCategoryIds() != null && !request.getCategoryIds().isEmpty(), ProductSpecification.hasCategory(request.getCategoryIds()));
        spec = andIf(spec, request.getBrandIds() != null && !request.getBrandIds().isEmpty(), ProductSpecification.hasBrand(request.getBrandIds()));
        spec = andIf(spec, request.getLinkStatus() != null, ProductSpecification.hasLinkStatus(request.getLinkStatus()));

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

        String name      = request.getName()           != null ? request.getName()           : product.getName();
        String imageUrl  = request.getImageUrl()       != null ? request.getImageUrl()       : product.getImageUrl();
        BigDecimal score = request.getNutritionScore() != null ? request.getNutritionScore() : product.getNutritionScore();

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

    @Transactional
    public void resetAll() {

        entityManager.createNativeQuery("TRUNCATE TABLE user_favorites RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE review_images RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE reviews RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE coupang_links RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE product_view_logs RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE product_nutrients RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE products RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE brands RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM categories WHERE depth = 3").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM categories WHERE depth = 2").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM categories WHERE depth = 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE categories_id_seq RESTART WITH 1").executeUpdate();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private Specification<Product> andIf(Specification<Product> spec, boolean condition, Specification<Product> other) {
        return condition ? spec.and(other) : spec;
    }

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

    private List<NutrientClaim> parseClaims(List<String> rawClaims) {
        if (rawClaims == null || rawClaims.isEmpty()) return List.of();
        return rawClaims.stream()
                .map(s -> {
                    try {
                        return NutrientClaim.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new CustomException(ErrorCode.INVALID_QUERY_PARAM,
                                "유효하지 않은 영양 강조표시 값: " + s);
                    }
                })
                .collect(Collectors.toList());
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case "POPULAR"     -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "SCORE"       -> Sort.by(Sort.Direction.DESC, "nutritionScore");
            case "ACCURACY",
                 "RECOMMENDED" -> Sort.by(Sort.Direction.DESC, "createdAt");
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
                .isFavorited(favorited)
                .scoreRankPercent(null)
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
                        .build())
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
                        : "UNLINKED")
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