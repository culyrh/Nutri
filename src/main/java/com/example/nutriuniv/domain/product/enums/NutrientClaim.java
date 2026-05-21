package com.example.nutriuniv.domain.product.enums;

import lombok.Getter;

@Getter
public enum NutrientClaim {

    LOW_CALORIE("저열량", "열량", "저"),
    NO_CALORIE("무열량", "열량", "무"),

    LOW_SODIUM("저나트륨", "나트륨", "저"),
    NO_SODIUM("무나트륨", "나트륨", "무"),

    LOW_SUGAR("저당", "당류", "저"),
    NO_SUGAR("무당", "당류", "무"),

    LOW_FAT("저지방", "지방", "저"),
    NO_FAT("무지방", "지방", "무"),

    LOW_TRANS_FAT("저트랜스지방", "트랜스지방", "저"),

    LOW_SATURATED_FAT("저포화지방", "포화지방", "저"),
    NO_SATURATED_FAT("무포화지방", "포화지방", "무"),

    LOW_CHOLESTEROL("저콜레스테롤", "콜레스테롤", "저"),
    NO_CHOLESTEROL("무콜레스테롤", "콜레스테롤", "무"),

    FIBER_SOURCE("식이섬유 함유", "식이섬유", "함유"),
    HIGH_FIBER("고식이섬유", "식이섬유", "고"),

    PROTEIN_SOURCE("단백질 함유", "단백질", "함유"),
    HIGH_PROTEIN("고단백", "단백질", "고");

    private final String label;
    private final String nutrient;
    private final String claimLevel;

    NutrientClaim(String label, String nutrient, String claimLevel) {
        this.label      = label;
        this.nutrient   = nutrient;
        this.claimLevel = claimLevel;
    }
}