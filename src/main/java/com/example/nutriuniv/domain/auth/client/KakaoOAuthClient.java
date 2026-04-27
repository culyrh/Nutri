package com.example.nutriuniv.domain.auth.client;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    /**
     * 카카오 인가 코드 → access token 교환
     * POST https://kauth.kakao.com/oauth/token
     */
    @Override
    public String getAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://kauth.kakao.com/oauth/token", request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "카카오 토큰 교환에 실패했습니다. 원인: " + e.getMessage());
        }
    }

    /**
     * 카카오 access token → 유저 정보 조회
     * GET https://kapi.kakao.com/v2/user/me
     *
     * 카카오 응답 구조:
     * {
     *   "id": 123456789,
     *   "kakao_account": {
     *     "email": "user@kakao.com",       // 선택 동의 - null 가능
     *     "profile": {
     *       "nickname": "홍길동"            // 선택 동의 - null 가능
     *     }
     *   }
     * }
     */
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            String oauthId = String.valueOf(body.get("id")); // Long → String

            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            String email = null;
            String name = null;

            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email"); // 선택 동의라 null 가능

                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }

            return new OAuthUserInfo(oauthId, email, name);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "카카오 유저 정보 조회에 실패했습니다.");
        }
    }
}