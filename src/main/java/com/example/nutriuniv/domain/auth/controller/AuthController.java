package com.example.nutriuniv.domain.auth.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.auth.dto.OAuthLoginRequest;
import com.example.nutriuniv.domain.auth.dto.RefreshRequest;
import com.example.nutriuniv.domain.auth.dto.RegisterRequest;
import com.example.nutriuniv.domain.auth.dto.TokenResponse;
import com.example.nutriuniv.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소셜 로그인",
            description = "기존회원이면 토큰 반환. 신규회원이면 is_new_user=true + googleEmail + oauthId 반환 (토큰 없음).")
    @PostMapping("/oauth")
    public ResponseEntity<CommonResponse<TokenResponse>> login(@RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.login(request)));
    }

    @Operation(summary = "회원가입",
            description = "신규회원이 첫번째 랜딩페이지에서 이름/이메일/성별/생년월일 입력 후 호출. 토큰 반환.")
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<TokenResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.register(request)));
    }

    @Operation(summary = "액세스 토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<String>> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.refresh(request)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(CommonResponse.success(null));
    }
}