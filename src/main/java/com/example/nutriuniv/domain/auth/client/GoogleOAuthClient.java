package com.example.nutriuniv.domain.auth.client;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import lombok.Getter;
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
public class GoogleOAuthClient {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    // 인가코드 → 구글 access_token 교환
    public String getAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "구글 토큰 교환에 실패했습니다. 원인: " + e.toString());
        }
    }

    // 구글 access_token → 유저 정보 조회
    public GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            return new GoogleUserInfo(
                    (String) body.get("id"),
                    (String) body.get("email"),
                    (String) body.get("name")
            );
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LOGIN_FAILED, "구글 유저 정보 조회에 실패했습니다.");
        }
    }

    @Getter
    public static class GoogleUserInfo {
        private final String oauthId;
        private final String email;
        private final String name;

        public GoogleUserInfo(String oauthId, String email, String name) {
            this.oauthId = oauthId;
            this.email = email;
            this.name = name;
        }
    }
}