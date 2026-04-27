package com.example.nutriuniv.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
    private String email;    // 신규회원일 때만. provider 무관하게 공통 필드로 변경 (구 googleEmail)
    private String oauthId;  // 신규회원일 때만 (register 호출에 필요)
}