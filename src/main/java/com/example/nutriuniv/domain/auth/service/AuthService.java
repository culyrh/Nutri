package com.example.nutriuniv.domain.auth.service;

import com.example.nutriuniv.common.exception.CustomException;
import com.example.nutriuniv.common.exception.ErrorCode;
import com.example.nutriuniv.common.security.JwtService;
import com.example.nutriuniv.domain.auth.client.GoogleOAuthClient;
import com.example.nutriuniv.domain.auth.dto.OAuthLoginRequest;
import com.example.nutriuniv.domain.auth.dto.RefreshRequest;
import com.example.nutriuniv.domain.auth.dto.TokenResponse;
import com.example.nutriuniv.domain.auth.entity.AuthToken;
import com.example.nutriuniv.domain.auth.repository.AuthTokenRepository;
import com.example.nutriuniv.domain.user.entity.User;
import com.example.nutriuniv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtService jwtService;
    private final GoogleOAuthClient googleOAuthClient;

    @Transactional
    public TokenResponse login(OAuthLoginRequest request) {

        // 구글 인가코드 → access_token → 유저 정보
        String googleAccessToken = googleOAuthClient.getAccessToken(request.getCode());
        GoogleOAuthClient.GoogleUserInfo userInfo = googleOAuthClient.getUserInfo(googleAccessToken);

        Optional<User> existing = userRepository.findByOauthProviderAndOauthId(
                request.getProvider(), userInfo.getOauthId());

        boolean isNewUser = existing.isEmpty();
        User user;

        if (isNewUser) {
            if (request.getName() == null || request.getGender() == null || request.getBirthDate() == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST, "신규 회원은 name, gender, birth_date가 필요합니다.");
            }
            user = userRepository.save(User.create(
                    request.getProvider(),
                    userInfo.getOauthId(),
                    request.getName(),
                    request.getGender(),
                    request.getBirthDate()
            ));
        } else {
            user = existing.get();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 기존 토큰 교체 (rotation)
        authTokenRepository.deleteByUserId(user.getId());
        authTokenRepository.save(AuthToken.create(
                user, refreshToken, null,
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpMs() * 1_000_000)
        ));

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .build();
    }

    @Transactional
    public String refresh(RefreshRequest request) {
        AuthToken authToken = authTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(authToken);
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = authToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        authToken.rotate(newRefreshToken,
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpMs() * 1_000_000));

        return newAccessToken;
    }

    @Transactional
    public void logout(Long userId) {
        authTokenRepository.deleteByUserId(userId);
    }
}