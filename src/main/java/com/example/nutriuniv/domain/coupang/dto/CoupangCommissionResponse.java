package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoupangCommissionResponse {
    private String rCode;
    private List<CoupangCommissionData> data;
}
