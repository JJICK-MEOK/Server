package com.jjikmeok.app.domain.auth.service;

import java.net.URI;
import java.util.StringJoiner;

import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthClient;
import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthRes;
import com.jjikmeok.app.domain.auth.config.GoogleOAuthProperties;
import com.jjikmeok.app.domain.auth.store.OAuthStateStore;
import com.jjikmeok.app.domain.auth.token.SecureTokenGenerator;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GoogleOAuthHandoffService {

    private static final String PLATFORM_WEB = "web";
    private static final String PLATFORM_APP = "app";

    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final OAuthStateStore oAuthStateStore;
    private final SecureTokenGenerator secureTokenGenerator;
    private final OAuthHandoffCommonService oAuthHandoffCommonService;

    public URI createGoogleLoginUri(final String platform) {
        final String state = secureTokenGenerator.generateUrlSafeToken(googleOAuthProperties.getStateTokenBytes());
        oAuthStateStore.save(state, normalizePlatform(platform), googleOAuthProperties.getStateTtl());

        log.debug("구글 OAuth 로그인 URL 생성 완료.");
        return UriComponentsBuilder.fromUriString(googleOAuthProperties.getAuthorizationUri())
                .queryParam("client_id", googleOAuthProperties.getClientId())
                .queryParam("redirect_uri", googleOAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", createScopeValue())
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUri();
    }

    @Transactional
    public URI handleGoogleCallback(final String code, final String state, final String error) {
        final String platform = validateState(state);
        validateCallbackError(error);
        validateCode(code);

        final GoogleOAuthRes.TokenResponse tokenResponse = googleOAuthClient.getToken(code);
        final GoogleOAuthRes.UserInfoResponse userInfo = googleOAuthClient.getUserInfo(tokenResponse.accessToken());
        final OAuthUserResult userResult = findOrCreateUser(userInfo);
        final String handoffToken = oAuthHandoffCommonService.createHandoffToken(
                userResult,
                googleOAuthProperties.getHandoffTokenBytes(),
                googleOAuthProperties.getHandoffTtl()
        );

        log.debug("구글 OAuth 콜백 처리 완료. userId={}, newMember={}",
                userResult.user().getId(), userResult.newMember());
        return createRedirectUri(handoffToken, platform);
    }

    private String normalizePlatform(final String platform) {
        if (platform != null && PLATFORM_WEB.equalsIgnoreCase(platform.trim())) {
            return PLATFORM_WEB;
        }
        return PLATFORM_APP;
    }

    private String createScopeValue() {
        final StringJoiner joiner = new StringJoiner(" ");
        googleOAuthProperties.getScopes().forEach(joiner::add);
        return joiner.toString();
    }

    private void validateCallbackError(final String error) {
        if (error == null || error.isBlank()) {
            return;
        }
        if ("access_denied".equals(error)) {
            log.warn("구글 OAuth 콜백 실패 - 사용자가 로그인을 취소했습니다.");
            throw new CustomException(ErrorCode.AUTH_GOOGLE_LOGIN_CANCELLED);
        }
        log.warn("구글 OAuth 콜백 실패 - OAuth 제공자 오류가 전달되었습니다. error={}", error);
        throw new CustomException(ErrorCode.AUTH_GOOGLE_CALLBACK_FAILED);
    }

    private String validateState(final String state) {
        if (state == null || state.isBlank()) {
            log.warn("구글 OAuth 콜백 실패 - 유효하지 않은 state입니다.");
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }

        final String platform = oAuthStateStore.consumeAndGet(state);
        if (platform == null) {
            log.warn("구글 OAuth 콜백 실패 - 유효하지 않은 state입니다.");
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }
        return platform;
    }

    private void validateCode(final String code) {
        if (code == null || code.isBlank()) {
            log.warn("구글 OAuth 콜백 실패 - 인가 코드가 없습니다.");
            throw new CustomException(ErrorCode.AUTH_GOOGLE_CALLBACK_FAILED);
        }
    }

    private OAuthUserResult findOrCreateUser(final GoogleOAuthRes.UserInfoResponse userInfo) {
        return oAuthHandoffCommonService.findOrCreateUser(
                AuthProvider.GOOGLE,
                userInfo.sub(),
                userInfo.email()
        );
    }

    private URI createRedirectUri(final String handoffToken, final String platform) {
        String baseUri = googleOAuthProperties.getAppDeepLinkUri();
        if (PLATFORM_WEB.equals(platform)) {
            baseUri = googleOAuthProperties.getWebRedirectUri();
        }

        return UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("handoffToken", handoffToken)
                .build()
                .encode()
                .toUri();
    }

}
