package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoupangCancelResponse {
    private String rCode;
    private List<CoupangCancelData> data;
}
