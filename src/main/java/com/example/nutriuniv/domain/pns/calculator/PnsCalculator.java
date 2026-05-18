package com.example.nutriuniv.domain.pns.calculator;

import java.math.BigDecimal;

/**
 * PFS 점수 산출 (Hwang et al. 2025) + KDRI 2025.
 *
 * Eq.1 (적정 범위, 탄·단·지)
 * Eq.2 (양↑좋음, 식이섬유)
 * Eq.3 (콜레스테롤)
 * Eq.4 (양↑나쁨, 포화·트랜스·당·나트륨)
 * Eq.5 PFS = Σ 9개 영양소 점수
 *
 * 입력 영양값 단위 (1회 제공량 기준):
 *   carb/protein/fat/fiber/satFat/transFat/sugar : g
 *   cholesterol : mg
 *   sodium      : mg
 */
public final class PnsCalculator {

    private PnsCalculator() {}

    public static class Result {
        public final double score;
        public final String grade;

        Result(double score, String grade) {
            this.score = score;
            this.grade = grade;
        }
    }

    public static Result calculate(int eerBand, double mealRatio,
                                   BigDecimal carb, BigDecimal protein, BigDecimal fat,
                                   BigDecimal fiber, BigDecimal cholesterol,
                                   BigDecimal satFat, BigDecimal transFat,
                                   BigDecimal sugar, BigDecimal sodium) {
        double eer = eerBand;

        // 일일 권장량 → 1회 섭취 기준
        double carbMin   = eer * 0.50 / 4 * mealRatio;
        double carbMax   = eer * 0.65 / 4 * mealRatio;
        double protMin   = eer * 0.10 / 4 * mealRatio;
        double protMax   = eer * 0.20 / 4 * mealRatio;
        double fatMin    = eer * 0.15 / 9 * mealRatio;
        double fatMax    = eer * 0.30 / 9 * mealRatio;
        double fiberMin  = eer * 12.0 / 1000 * mealRatio;
        double cholMax   = 300.0 * mealRatio;
        double satMax    = eer * 0.07 / 9 * mealRatio;
        double transMax  = eer * 0.01 / 9 * mealRatio;
        double sugarMax  = eer * 0.10 / 4 * mealRatio;
        double sodiumMax = 2300.0 * mealRatio;

        double s = 0.0;
        s += eq1(asDouble(carb),    carbMin, carbMax);
        s += eq1(asDouble(protein), protMin, protMax);
        s += eq1(asDouble(fat),     fatMin,  fatMax);
        s += eq2(asDouble(fiber),   fiberMin);
        s += eq3(asDouble(cholesterol), cholMax);
        s += eq4(asDouble(satFat),   satMax);
        s += eq4(asDouble(transFat), transMax);
        s += eq4(asDouble(sugar),    sugarMax);
        s += eq4(asDouble(sodium),   sodiumMax);

        return new Result(s, toGrade(s));
    }

    /** Eq.1 — Nx<Nmin: 1-((Nx-Nmin)/Nmin)², Nmin~Nmax: 1, Nx>Nmax: 1-((Nx-Nmax)/Nmin)² */
    private static double eq1(double nx, double nmin, double nmax) {
        if (nx < nmin) {
            double r = (nx - nmin) / nmin;
            return 1 - r * r;
        }
        if (nx <= nmax) return 1.0;
        double r = (nx - nmax) / nmin;  // ⚠ 명세 §5: 분모는 Nmax가 아니라 Nmin
        return 1 - r * r;
    }

    /** Eq.2 — 식이섬유: Nx<Nmin: 1-((Nx-Nmin)/Nmin)², Nx>=Nmin: 1 */
    private static double eq2(double nx, double nmin) {
        if (nx >= nmin) return 1.0;
        double r = (nx - nmin) / nmin;
        return 1 - r * r;
    }

    /** Eq.3 — 콜레스테롤: Nx<Nmax: -1+((Nx-Nmax)/Nmax)², Nx>=Nmax: -1 */
    private static double eq3(double nx, double nmax) {
        if (nx >= nmax) return -1.0;
        double r = (nx - nmax) / nmax;
        return -1 + r * r;
    }

    /** Eq.4 — 양↑나쁨: -(1/Nmax) × Nx (선형 감점, 하한 없음) */
    private static double eq4(double nx, double nmax) {
        return -(nx / nmax);
    }

    /** 명세 §6 잠정 등급 구간. 출시 후 분포 보고 조정. */
    private static String toGrade(double score) {
        if (score >=  5.0)   return "A";
        if (score >=  2.0)   return "B";
        if (score >= -1.0)   return "C";
        if (score >= -4.0)   return "D";
        return "E";
    }

    private static double asDouble(BigDecimal v) {
        return v == null ? 0.0 : v.doubleValue();
    }
}
