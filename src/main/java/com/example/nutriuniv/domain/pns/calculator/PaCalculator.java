package com.example.nutriuniv.domain.pns.calculator;

public final class PaCalculator {

    private PaCalculator() {}

    public static PaLevel calculate(String activityType, int weeklyExerciseCount, String exerciseIntensity) {
        int score = activityScore(activityType)
                  + frequencyScore(weeklyExerciseCount)
                  + intensityScore(exerciseIntensity);
        return PaLevel.fromScore(score);
    }

    private static int activityScore(String raw) {
        if (raw == null) return 1;
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "PHYSICAL", "LABOR", "육체노동", "육체"             -> 3;
            case "STANDING", "STAND",  "서서"                       -> 2;
            default                                                  -> 1;
        };
    }

    private static int frequencyScore(int weekly) {
        if (weekly >= 5) return 2;
        if (weekly >= 2) return 1;
        return 0;
    }

    private static int intensityScore(String raw) {
        if (raw == null) return 0;
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "STRONG", "HARD", "HIGH", "강하게", "강"  -> 2;
            case "MEDIUM", "MID",  "중간",  "중"           -> 1;
            default                                        -> 0;
        };
    }
}
