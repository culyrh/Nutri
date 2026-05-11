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
public class NaverOAuthClient implements OAuthClient {

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
    }

    /**
     * 네이버 인가 코드 → access token 교환
     * POST https://nid.naver.com/oauth2.0/token
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
                    "https://nid.naver.com/oauth2.0/token", request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "네이버 토큰 교환에 실패했습니다. 원인: " + e.getMessage());
        }
    }

    /**
     * 네이버 access token → 유저 정보 조회
     * GET https://openapi.naver.com/v1/nid/me
     *
     * 네이버 응답 구조:
     * {
     *   "resultcode": "00",
     *   "message": "success",
     *   "response": {
     *     "id": "32742776",
     *     "email": "user@naver.com",
     *     "name": "홍길동"
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
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            Map<String, Object> naverResponse = (Map<String, Object>) body.get("response");

            String oauthId = (String) naverResponse.get("id");
            String email = (String) naverResponse.get("email");
            String name = (String) naverResponse.get("name");

            return new OAuthUserInfo(oauthId, email, name);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "네이버 유저 정보 조회에 실패했습니다.");
        }
    }
}