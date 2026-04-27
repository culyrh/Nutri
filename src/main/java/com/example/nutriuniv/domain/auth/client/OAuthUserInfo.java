package com.example.nutriuniv.domain.auth.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 OAuth 프로바이더에서 공통으로 사용하는 유저 정보 DTO.
 * 각 OAuthClient 구현체가 provider별 응답을 이 객체로 변환해서 반환한다.
 */
@Getter
@RequiredArgsConstructor
public class OAuthUserInfo {
    private final String oauthId;
    private final String email;     // 카카오는 선택 동의라 null일 수 있음
    private final String name;      // 일부 provider는 null일 수 있음
}