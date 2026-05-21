package com.example.nutriuniv.domain.product.dto;

import com.example.nutriuniv.domain.product.enums.NutrientClaim;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class NutrientClaimResponse {

    private String code;
    private String label;
    private String nutrient;
    private String claimLevel;

    public static List<NutrientClaimResponse> getAllClaims() {
        return Arrays.stream(NutrientClaim.values())
                .map(c -> NutrientClaimResponse.builder()
                        .code(c.name())
                        .label(c.getLabel())
                        .nutrient(c.getNutrient())
                        .claimLevel(c.getClaimLevel())
                        .build())
                .collect(Collectors.toList());
    }
}