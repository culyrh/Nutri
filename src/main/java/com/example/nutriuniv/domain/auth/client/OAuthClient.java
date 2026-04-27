package com.example.nutriuniv.domain.auth.client;

/**
 * OAuth 프로바이더별 클라이언트 인터페이스.
 * Google, Kakao, Naver, Apple 각 구현체는 이 인터페이스를 구현한다.
 */
public interface OAuthClient {

    OAuthProvider getProvider();

    /**
     * 인가 코드(authorization code)를 프로바이더 access token으로 교환한다.
     */
    String getAccessToken(String code);

    /**
     * 프로바이더 access token으로 유저 정보를 조회한다.
     */
    OAuthUserInfo getUserInfo(String accessToken);
}