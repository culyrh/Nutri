package com.example.nutriuniv.domain.pns.calculator;

import java.util.Set;

/**
 * 명세 §11.4 — 음료/스낵·빵·디저트/견과류 = 0.1, 나머지 = 0.3.
 *
 * 대분류 ID(depth=1):
 *   524 음료류, 526 쉐이크·프로틴, 529 스낵·빵·디저트, 531 닭고기, 534 유제품,
 *   537 견과류, 545 유지류·소스, 547 밥·식사류, 550 곡류·시리얼, 552 면류,
 *   566 수산물, 581 돼지·소·오리, 594 식물성 단백질
 */
public final class MealRatioResolver {

    private static final Set<Long> SNACK_LIKE_PARENTS = Set.of(
            524L,  // 음료류
            529L,  // 스낵·빵·디저트
            537L   // 견과류
    );

    private static final double SNACK_RATIO = 0.1;
    private static final double MAIN_RATIO  = 0.3;

    private MealRatioResolver() {}

    public static double resolve(Long parentCategoryId) {
        if (parentCategoryId != null && SNACK_LIKE_PARENTS.contains(parentCategoryId)) {
            return SNACK_RATIO;
        }
        return MAIN_RATIO;
    }
}
