package com.example.nutriuniv.domain.coupang.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoupangClickResponse {
    private String rCode;
    private List<CoupangClickData> data;
}
