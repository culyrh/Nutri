package com.example.nutriuniv.domain.pns.calculator;

public enum Gender {
    MALE, FEMALE;

    public static Gender from(String raw) {
        if (raw == null) return FEMALE;
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "M", "MALE", "남", "남성" -> MALE;
            default -> FEMALE;
        };
    }
}
