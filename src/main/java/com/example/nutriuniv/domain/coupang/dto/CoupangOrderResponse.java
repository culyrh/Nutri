package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoupangOrderResponse {
    private String rCode;
    private List<CoupangOrderData> data;
}
