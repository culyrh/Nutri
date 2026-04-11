package com.example.nutriuniv.domain.product.specification;

import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
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

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
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
}