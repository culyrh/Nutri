package com.example.nutriuniv.domain.pns.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * EER (Estimated Energy Requirement) — KDRI 2025 / PFS 논문(Hwang et al. 2025) 공식.
 *   EER = α + PA × (β × Age + γ × Weight + δ × Height(m))
 *
 * ⚠ 신장은 cm 입력을 m로 변환 후 대입 (명세서 §11.1).
 */
public final class EerCalculator {

    private static final int MIN_BAND = 1500;
    private static final int MAX_BAND = 3000;
    private static final int BAND_STEP = 500;

    private EerCalculator() {}

    public static double calculate(Gender gender, LocalDate birthDate,
                                   BigDecimal heightCm, BigDecimal weightKg, PaLevel pa) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        double height = heightCm.doubleValue() / 100.0;
        double weight = weightKg.doubleValue();
        double paFactor = pa.factor(gender);

        double a, b, c, d;
        if (gender == Gender.MALE) {
            a = 662.0; b = -9.53; c = 15.91; d = 539.6;
        } else {
            a = 354.0; b = -6.91; c =  9.36; d = 726.0;
        }
        return a + paFactor * (b * age + c * weight + d * height);
    }

    /**
     * EER을 1500/2000/2500/3000 4구간 중 가장 가까운 값으로 매핑.
     * 경계(1750/2250/2750)는 위로 올림. 범위 밖은 클램프.
     */
    public static int toBand(double eer) {
        if (eer <= MIN_BAND) return MIN_BAND;
        if (eer >= MAX_BAND) return MAX_BAND;
        double rel = (eer - MIN_BAND) / BAND_STEP;
        int idx = (int) Math.floor(rel + 0.5);  // 0.5는 위로 올림
        return MIN_BAND + idx * BAND_STEP;
    }
}
