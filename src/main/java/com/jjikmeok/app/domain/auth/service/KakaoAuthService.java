package com.jjikmeok.app.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthClient;
import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthRes;
import com.jjikmeok.app.domain.auth.dto.response.LoginRes;
import com.jjikmeok.app.domain.auth.store.RefreshTokenStore;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.security.jwt.JwtProperties;
import com.jjikmeok.app.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public LoginRes kakaoLogin(final String code) {
        final String kakaoAccessToken = kakaoOAuthClient.getAccessToken(code);
        final KakaoOAuthRes.UserInfoResponse userInfo = kakaoOAuthClient.getUserInfo(kakaoAccessToken);
        final User user = findOrCreateUser(userInfo);

        final Long userId = user.getId();
        final String role = AuthUtils.resolveRole(user.getRole());
        final String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        final String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        final int expiresIn = AuthUtils.accessTokenExpiresInSeconds(jwtProperties);

        refreshTokenStore.saveToken(userId, refreshToken, AuthUtils.refreshTokenTtl(jwtProperties));

        return new LoginRes(accessToken, refreshToken, TOKEN_TYPE, expiresIn, user.getRegistrationStatus());
    }

    private User findOrCreateUser(final KakaoOAuthRes.UserInfoResponse userInfo) {
        final String providerId = String.valueOf(userInfo.id());
        final String email = AuthUtils.normalizeEmail(userInfo.kakaoAccount().email());

        return userRepository.findByAuthProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .orElseGet(() -> {
                    final User savedUser = userRepository.save(User.createForOAuth2(email, AuthProvider.KAKAO, providerId));
                    log.info("Kakao OAuth signup completed. userId={}, providerId={}", savedUser.getId(), providerId);
                    return savedUser;
                });
    }
}
