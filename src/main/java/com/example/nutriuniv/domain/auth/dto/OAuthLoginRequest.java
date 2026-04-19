package com.example.nutriuniv.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class OAuthLoginRequest {
    private String provider;
    private String code;        // 구글 인가코드
    // 신규회원만 필요
    private String name;
    private String gender;
    private LocalDate birthDate;
}