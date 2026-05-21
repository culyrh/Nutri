package com.example.nutriuniv.domain.product.specification;

import com.example.nutriuniv.domain.product.entity.Product;
import com.example.nutriuniv.domain.product.entity.ProductNutrient;
import com.example.nutriuniv.domain.product.enums.NutrientClaim;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

/**
 * 영양 강조표시 기준 Specification (식품표시 등에 관한 법률)
 *
 * serving_size ILIKE '%ml%' → 액체 → per100mL 기준
 * serving_size ILIKE '%g%'  → 고체 → per100g 기준
 * 기타(1마리 등 13건)       → 필터 제외
 */
public class NutrientClaimSpecification {

    @SuppressWarnings("unchecked")
    private static Join<Product, ProductNutrient> getNutrientJoin(Root<Product> root) {
        return root.getJoins().stream()
                .filter(j -> "productNutrient".equals(j.getAttribute().getName()))
                .map(j -> (Join<Product, ProductNutrient>) j)
                .findFirst()
                .orElseGet(() -> root.join("productNutrient", JoinType.LEFT));
    }

    /**
     * 여러 NutrientClaim을 AND 조건으로 결합
     */
    public static Specification<Product> hasClaims(List<NutrientClaim> claims) {
        if (claims == null || claims.isEmpty()) return null;

        Specification<Product> combined = Specification.where((Specification<Product>) null);
        for (NutrientClaim claim : claims) {
            combined = combined.and(forClaim(claim));
        }
        return combined;
    }

    public static Specification<Product> forClaim(NutrientClaim claim) {
        return switch (claim) {
            case LOW_CALORIE       -> lowCalorie();
            case NO_CALORIE        -> noCalorie();
            case LOW_SODIUM        -> lowSodium();
            case NO_SODIUM         -> noSodium();
            case LOW_SUGAR         -> lowSugar();
            case NO_SUGAR          -> noSugar();
            case LOW_FAT           -> lowFat();
            case NO_FAT            -> noFat();
            case LOW_TRANS_FAT     -> lowTransFat();
            case LOW_SATURATED_FAT -> lowSaturatedFat();
            case NO_SATURATED_FAT  -> noSaturatedFat();
            case LOW_CHOLESTEROL   -> lowCholesterol();
            case NO_CHOLESTEROL    -> noCholesterol();
            case FIBER_SOURCE      -> fiberSource();
            case HIGH_FIBER        -> highFiber();
            case PROTEIN_SOURCE    -> proteinSource();
            case HIGH_PROTEIN      -> highProtein();
        };
    }

    // ── 열량 ────────────────────────────────────────────────────────────────────

    /** 저열량: 고체 100g당 40kcal 미만 OR 액체 100mL당 20kcal 미만 */
    private static Specification<Product> lowCalorie() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");
            return cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("caloriesPer100g"), new BigDecimal("20"))),
                    cb.and(isSolid,  cb.lessThan(n.get("caloriesPer100g"), new BigDecimal("40")))
            );
        };
    }

    /** 무열량: 액체 100mL당 4kcal 미만 */
    private static Specification<Product> noCalorie() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                    cb.lessThan(n.get("caloriesPer100g"), new BigDecimal("4"))
            );
        };
    }

    // ── 나트륨 ──────────────────────────────────────────────────────────────────

    /** 저나트륨: 100g당 120mg 미만 */
    private static Specification<Product> lowSodium() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.or(cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                            cb.like(cb.lower(n.get("servingSize")), "%g%")),
                    cb.lessThan(n.get("sodiumPer100g"), new BigDecimal("120"))
            );
        };
    }

    /** 무나트륨: 100g당 5mg 미만 */
    private static Specification<Product> noSodium() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.or(cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                            cb.like(cb.lower(n.get("servingSize")), "%g%")),
                    cb.lessThan(n.get("sodiumPer100g"), new BigDecimal("5"))
            );
        };
    }

    // ── 당류 ────────────────────────────────────────────────────────────────────

    /** 저당: 고체 100g당 5g 미만 OR 액체 100mL당 2.5g 미만 */
    private static Specification<Product> lowSugar() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");
            return cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("sugarPer100g"), new BigDecimal("2.5"))),
                    cb.and(isSolid,  cb.lessThan(n.get("sugarPer100g"), new BigDecimal("5")))
            );
        };
    }

    /** 무당: 100g(mL)당 0.5g 미만 */
    private static Specification<Product> noSugar() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.or(cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                            cb.like(cb.lower(n.get("servingSize")), "%g%")),
                    cb.lessThan(n.get("sugarPer100g"), new BigDecimal("0.5"))
            );
        };
    }

    // ── 지방 ────────────────────────────────────────────────────────────────────

    /** 저지방: 고체 100g당 3g 미만 OR 액체 100mL당 1.5g 미만 */
    private static Specification<Product> lowFat() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");
            return cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("fatPer100g"), new BigDecimal("1.5"))),
                    cb.and(isSolid,  cb.lessThan(n.get("fatPer100g"), new BigDecimal("3")))
            );
        };
    }

    /** 무지방: 100g(mL)당 0.5g 미만 */
    private static Specification<Product> noFat() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.or(cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                            cb.like(cb.lower(n.get("servingSize")), "%g%")),
                    cb.lessThan(n.get("fatPer100g"), new BigDecimal("0.5"))
            );
        };
    }

    // ── 트랜스지방 ──────────────────────────────────────────────────────────────

    /** 저트랜스지방: 100g당 0.5g 미만 */
    private static Specification<Product> lowTransFat() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            return cb.and(
                    cb.or(cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                            cb.like(cb.lower(n.get("servingSize")), "%g%")),
                    cb.lessThan(n.get("transFatPer100g"), new BigDecimal("0.5"))
            );
        };
    }

    // ── 포화지방 ────────────────────────────────────────────────────────────────

    /**
     * 저포화지방: (고체 100g당 1.5g 미만 OR 액체 100mL당 0.75g 미만)
     *            AND 포화지방이 열량의 10% 미만
     */
    private static Specification<Product> lowSaturatedFat() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");

            Predicate amountOk = cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("0.75"))),
                    cb.and(isSolid,  cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("1.5")))
            );
            // 포화지방(g) * 9kcal < 총열량 * 10%  →  saturatedFat * 9 < calories * 0.1
            Predicate ratioOk = cb.lessThan(
                    cb.prod(n.<BigDecimal>get("saturatedFatPer100g"), new BigDecimal("9")),
                    cb.prod(n.<BigDecimal>get("caloriesPer100g"),     new BigDecimal("0.1"))
            );
            return cb.and(cb.or(isLiquid, isSolid), amountOk, ratioOk);
        };
    }

    /**
     * 무포화지방: 고체 100g당 1.5g 미만 OR 액체 100mL당 0.1g 미만
     */
    private static Specification<Product> noSaturatedFat() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");
            return cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("0.1"))),
                    cb.and(isSolid,  cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("1.5")))
            );
        };
    }

    // ── 콜레스테롤 ──────────────────────────────────────────────────────────────

    /**
     * 저콜레스테롤: (고체 100g당 20mg 미만 OR 액체 100mL당 10mg 미만)
     *             AND 포화지방 (고체 1.5g 미만 OR 액체 0.75g 미만)
     *             AND 포화지방이 열량의 10% 미만
     */
    private static Specification<Product> lowCholesterol() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");

            Predicate cholOk = cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("cholesterolPer100g"), new BigDecimal("10"))),
                    cb.and(isSolid,  cb.lessThan(n.get("cholesterolPer100g"), new BigDecimal("20")))
            );
            Predicate sfOk = cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("0.75"))),
                    cb.and(isSolid,  cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("1.5")))
            );
            Predicate ratioOk = cb.lessThan(
                    cb.prod(n.<BigDecimal>get("saturatedFatPer100g"), new BigDecimal("9")),
                    cb.prod(n.<BigDecimal>get("caloriesPer100g"),     new BigDecimal("0.1"))
            );
            return cb.and(cb.or(isLiquid, isSolid), cholOk, sfOk, ratioOk);
        };
    }

    /**
     * 무콜레스테롤: 100g(mL)당 5mg 미만
     *             AND 포화지방 (고체 1.5g 미만 OR 액체 0.75g 미만)
     *             AND 포화지방이 열량의 10% 미만
     */
    private static Specification<Product> noCholesterol() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");

            Predicate cholOk = cb.lessThan(n.get("cholesterolPer100g"), new BigDecimal("5"));
            Predicate sfOk = cb.or(
                    cb.and(isLiquid, cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("0.75"))),
                    cb.and(isSolid,  cb.lessThan(n.get("saturatedFatPer100g"), new BigDecimal("1.5")))
            );
            Predicate ratioOk = cb.lessThan(
                    cb.prod(n.<BigDecimal>get("saturatedFatPer100g"), new BigDecimal("9")),
                    cb.prod(n.<BigDecimal>get("caloriesPer100g"),     new BigDecimal("0.1"))
            );
            return cb.and(cb.or(isLiquid, isSolid), cholOk, sfOk, ratioOk);
        };
    }

    // ── 식이섬유 ────────────────────────────────────────────────────────────────

    /**
     * 식이섬유 함유: 100g당 3g 이상 OR 100kcal당 1.5g 이상
     * 100kcal당: fiberPer100g * 100 >= caloriesPer100g * 1.5
     */
    private static Specification<Product> fiberSource() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate hasUnit = cb.or(
                    cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                    cb.like(cb.lower(n.get("servingSize")), "%g%")
            );
            Predicate per100gOk = cb.greaterThanOrEqualTo(n.get("fiberPer100g"), new BigDecimal("3"));
            Predicate per100kcalOk = cb.greaterThanOrEqualTo(
                    cb.prod(n.<BigDecimal>get("fiberPer100g"),     new BigDecimal("100")),
                    cb.prod(n.<BigDecimal>get("caloriesPer100g"),  new BigDecimal("1.5"))
            );
            return cb.and(hasUnit, cb.or(per100gOk, per100kcalOk));
        };
    }

    /**
     * 고식이섬유: 함유 기준의 2배 → 100g당 6g 이상 OR 100kcal당 3g 이상
     */
    private static Specification<Product> highFiber() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate hasUnit = cb.or(
                    cb.like(cb.lower(n.get("servingSize")), "%ml%"),
                    cb.like(cb.lower(n.get("servingSize")), "%g%")
            );
            Predicate per100gOk = cb.greaterThanOrEqualTo(n.get("fiberPer100g"), new BigDecimal("6"));
            Predicate per100kcalOk = cb.greaterThanOrEqualTo(
                    cb.prod(n.<BigDecimal>get("fiberPer100g"),    new BigDecimal("100")),
                    cb.prod(n.<BigDecimal>get("caloriesPer100g"), new BigDecimal("3"))
            );
            return cb.and(hasUnit, cb.or(per100gOk, per100kcalOk));
        };
    }

    // ── 단백질 ──────────────────────────────────────────────────────────────────

    /**
     * 단백질 함유 (1일 기준치 55g):
     *   고체: 100g당 5.5g(기준치 10%) 이상
     *   액체: 100mL당 2.75g(기준치 5%) 이상
     *   공통: 100kcal당 2.75g(기준치 5%) 이상
     */
    private static Specification<Product> proteinSource() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");

            Predicate solidOk  = cb.and(isSolid,
                    cb.greaterThanOrEqualTo(n.get("proteinPer100g"), new BigDecimal("5.5")));
            Predicate liquidOk = cb.and(isLiquid,
                    cb.greaterThanOrEqualTo(n.get("proteinPer100g"), new BigDecimal("2.75")));
            Predicate per100kcalOk = cb.and(
                    cb.or(isLiquid, isSolid),
                    cb.greaterThanOrEqualTo(
                            cb.prod(n.<BigDecimal>get("proteinPer100g"),  new BigDecimal("100")),
                            cb.prod(n.<BigDecimal>get("caloriesPer100g"), new BigDecimal("2.75"))
                    )
            );
            return cb.or(solidOk, liquidOk, per100kcalOk);
        };
    }

    /**
     * 고단백: 함유 기준의 2배
     *   고체: 100g당 11g 이상 / 액체: 100mL당 5.5g 이상 / 100kcal당 5.5g 이상
     */
    private static Specification<Product> highProtein() {
        return (root, query, cb) -> {
            Join<Product, ProductNutrient> n = getNutrientJoin(root);
            Predicate isLiquid = cb.like(cb.lower(n.get("servingSize")), "%ml%");
            Predicate isSolid  = cb.like(cb.lower(n.get("servingSize")), "%g%");

            Predicate solidOk  = cb.and(isSolid,
                    cb.greaterThanOrEqualTo(n.get("proteinPer100g"), new BigDecimal("11")));
            Predicate liquidOk = cb.and(isLiquid,
                    cb.greaterThanOrEqualTo(n.get("proteinPer100g"), new BigDecimal("5.5")));
            Predicate per100kcalOk = cb.and(
                    cb.or(isLiquid, isSolid),
                    cb.greaterThanOrEqualTo(
                            cb.prod(n.<BigDecimal>get("proteinPer100g"),  new BigDecimal("100")),
                            cb.prod(n.<BigDecimal>get("caloriesPer100g"), new BigDecimal("5.5"))
                    )
            );
            return cb.or(solidOk, liquidOk, per100kcalOk);
        };
    }
}