package com.example.nutriuniv.domain.auth.client;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;

public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    NAVER,
    APPLE;

    public static OAuthProvider from(String value) {
        try {
            return OAuthProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "지원하지 않는 OAuth 프로바이더입니다: " + value);
        }
    }

    public String lowerName() {
        return name().toLowerCase();
    }
}