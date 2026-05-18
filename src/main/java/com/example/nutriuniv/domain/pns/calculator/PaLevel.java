package com.example.nutriuniv.domain.pns.calculator;

public enum PaLevel {
    SEDENTARY  (1.00, 1.00),
    LOW_ACTIVE (1.11, 1.12),
    ACTIVE     (1.25, 1.27),
    VERY_ACTIVE(1.48, 1.45);

    private final double male;
    private final double female;

    PaLevel(double male, double female) {
        this.male = male;
        this.female = female;
    }

    public double factor(Gender gender) {
        return gender == Gender.MALE ? male : female;
    }

    public static PaLevel fromScore(int total) {
        if (total <= 1) return SEDENTARY;
        if (total == 2) return LOW_ACTIVE;
        if (total <= 4) return ACTIVE;
        return VERY_ACTIVE;
    }
}
