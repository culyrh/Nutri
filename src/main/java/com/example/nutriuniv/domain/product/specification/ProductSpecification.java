package com.example.nutriuniv.domain.product.specification;

import com.example.nutriuniv.domain.category.entity.Category;
import com.example.nutriuniv.domain.coupang.entity.CoupangLink;
import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    // ────────────────────────────────────────────────────────────────────────────
    // JOIN 재사용 헬퍼
    //  - min/max 조건을 동시에 적용할 때 JOIN이 중복 생성되어 카르테시안 곱이 발생하는 문제 방지
    // ────────────────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private static Join<Product, ProductNutrient> getNutrientJoin(Root<Product> root) {
        return root.getJoins().stream()
                .filter(j -> "productNutrient".equals(j.getAttribute().getName()))
                .map(j -> (Join<Product, ProductNutrient>) j)
                .findFirst()
                .orElseGet(() -> root.join("productNutrient", JoinType.LEFT));
    }

    // ── 활성 상태 ─────────────────────────────────────────────────────────────────

    /** 일반 유저용: 활성 상품만 */
    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    /**
     * 관리자용: null → 전체, true → 활성만, false → 비활성만
     */
    public static Specification<Product> isActiveAdmin(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return isActive
                    ? cb.isTrue(root.get("isActive"))
                    : cb.isFalse(root.get("isActive"));
        };
    }

    // ── 공통 필터 ─────────────────────────────────────────────────────────────────

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) ->
                keyword == null ? null : cb.like(root.get("name"), "%" + keyword + "%");
    }

    /**
     * 카테고리 필터 - 선택한 카테고리 및 그 자식 카테고리 상품 모두 포함
     * depth1 선택 시 → 해당 카테고리 + 자식(depth2)들의 상품 조회
     * depth2 선택 시 → 해당 카테고리 상품만 조회 (자식 없음)
     */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;

            // 서브쿼리: parent_id = categoryId 인 자식 카테고리 id 목록
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Category> categoryRoot = subquery.from(Category.class);
            subquery.select(categoryRoot.get("id"))
                    .where(cb.equal(categoryRoot.get("parent").get("id"), categoryId));

            // category.id = categoryId OR category.id IN (자식 id 목록)
            return cb.or(
                    cb.equal(root.get("category").get("id"), categoryId),
                    root.get("category").get("id").in(subquery)
            );
        };
    }

    public static Specification<Product> hasBrand(Long brandId) {
        return (root, query, cb) ->
                brandId == null ? null : cb.equal(root.get("brand").get("id"), brandId);
    }

    // ── 칼로리 ───────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinCalories(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("calories"), min);
    }

    public static Specification<Product> hasMaxCalories(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("calories"), max);
    }

    // ── 단백질 ───────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinProtein(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("protein"), min);
    }

    public static Specification<Product> hasMaxProtein(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("protein"), max);
    }

    // ── 지방 ─────────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinFat(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("fat"), min);
    }

    public static Specification<Product> hasMaxFat(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("fat"), max);
    }

    // ── 탄수화물 ─────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinCarbohydrate(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("carbohydrate"), min);
    }

    public static Specification<Product> hasMaxCarbohydrate(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("carbohydrate"), max);
    }

    // ── 당 ───────────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinSugar(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("sugar"), min);
    }

    public static Specification<Product> hasMaxSugar(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("sugar"), max);
    }

    // ── 나트륨 ───────────────────────────────────────────────────────────────────

    public static Specification<Product> hasMinSodium(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(getNutrientJoin(root).get("sodium"), min);
    }

    public static Specification<Product> hasMaxSodium(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(getNutrientJoin(root).get("sodium"), max);
    }

    // ── 영양점수 ─────────────────────────────────────────────────────────────────
    // nutrition_score는 products 테이블 컬럼이므로 JOIN 불필요

    public static Specification<Product> hasMinNutritionScore(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(root.get("nutritionScore"), min);
    }

    public static Specification<Product> hasMaxNutritionScore(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(root.get("nutritionScore"), max);
    }

    // ── 쿠팡 링크 ─────────────────────────────────────────────────────────────────
    public static Specification<Product> hasLinkStatus(String linkStatus) {
        return (root, query, cb) -> {
            if (linkStatus == null) return null;
            Join<Product, CoupangLink> join = root.join("coupangLink", JoinType.LEFT);
            return cb.equal(join.get("linkStatus"), linkStatus);
        };
    }
}