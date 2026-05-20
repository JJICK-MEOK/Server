package com.jjikmeok.app.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthClient;
import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthRes;
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
public class GoogleAuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public LoginRes googleLogin(final String code) {
        final String googleAccessToken = googleOAuthClient.getAccessToken(code);
        final GoogleOAuthRes.UserInfoResponse userInfo = googleOAuthClient.getUserInfo(googleAccessToken);
        final User user = findOrCreateUser(userInfo);

        final Long userId = user.getId();
        final String role = AuthUtils.resolveRole(user.getRole());
        final String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        final String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        final int expiresIn = AuthUtils.accessTokenExpiresInSeconds(jwtProperties);

        refreshTokenStore.saveToken(userId, refreshToken, AuthUtils.refreshTokenTtl(jwtProperties));

        return new LoginRes(accessToken, refreshToken, TOKEN_TYPE, expiresIn);
    }

    private User findOrCreateUser(final GoogleOAuthRes.UserInfoResponse userInfo) {
        final String providerId = userInfo.sub();
        final String email = AuthUtils.normalizeEmail(userInfo.email());

        return userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> {
                    final User savedUser = userRepository.save(User.createForOAuth2(email, AuthProvider.GOOGLE, providerId));
                    log.info("Google OAuth signup completed. userId={}, providerId={}", savedUser.getId(), providerId);
                    return savedUser;
                });
    }
}
