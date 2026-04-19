package com.example.nutriuniv.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
    private String googleEmail;  // 신규회원일 때만, 기존회원은 NULL
    private String oauthId;      // 신규회원일 때만 (register 호출에 필요)
}