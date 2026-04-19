package com.example.nutriuniv.domain.auth.controller;

import com.example.nutriuniv.common.response.CommonResponse;
import com.example.nutriuniv.common.security.UserPrincipal;
import com.example.nutriuniv.domain.auth.dto.OAuthLoginRequest;
import com.example.nutriuniv.domain.auth.dto.RefreshRequest;
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

    @Operation(summary = "소셜 로그인 / 회원가입")
    @PostMapping("/oauth")
    public ResponseEntity<CommonResponse<TokenResponse>> login(@RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(CommonResponse.success(authService.login(request)));
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