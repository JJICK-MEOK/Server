package com.jjikmeok.app.domain.auth.service;

import java.net.URI;
import java.util.StringJoiner;

import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthClient;
import com.jjikmeok.app.domain.auth.client.google.GoogleOAuthRes;
import com.jjikmeok.app.domain.auth.config.GoogleOAuthProperties;
import com.jjikmeok.app.domain.auth.store.OAuthStateStore;
import com.jjikmeok.app.domain.auth.token.OAuthTokenGenerator;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthHandoffService {

    private final GoogleOAuthClient googleOAuthClient;
    private final GoogleOAuthProperties googleOAuthProperties;
    private final OAuthStateStore oAuthStateStore;
    private final OAuthTokenGenerator oAuthTokenGenerator;
    private final OAuthHandoffCommonService oAuthHandoffCommonService;

    public URI createGoogleLoginUri() {
        final String state = oAuthTokenGenerator.generateUrlSafeToken(googleOAuthProperties.getStateTokenBytes());
        oAuthStateStore.save(state, googleOAuthProperties.getStateTtl());

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
        validateCallbackError(error);
        validateState(state);
        validateCode(code);

        final GoogleOAuthRes.TokenResponse tokenResponse = googleOAuthClient.getToken(code);
        final GoogleOAuthRes.UserInfoResponse userInfo = googleOAuthClient.getUserInfo(tokenResponse.accessToken());
        final OAuthUserResult userResult = findOrCreateUser(userInfo);
        final String handoffToken = oAuthHandoffCommonService.createHandoffToken(
                userResult,
                googleOAuthProperties.getHandoffTokenBytes(),
                googleOAuthProperties.getHandoffTtl()
        );

        return createAppDeepLinkUri(handoffToken);
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
            throw new CustomException(ErrorCode.AUTH_GOOGLE_LOGIN_CANCELLED);
        }
        throw new CustomException(ErrorCode.AUTH_GOOGLE_CALLBACK_FAILED);
    }

    private void validateState(final String state) {
        if (state == null || state.isBlank() || !oAuthStateStore.consume(state)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }
    }

    private void validateCode(final String code) {
        if (code == null || code.isBlank()) {
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

    private URI createAppDeepLinkUri(final String handoffToken) {
        return UriComponentsBuilder.fromUriString(googleOAuthProperties.getAppDeepLinkUri())
                .queryParam("handoffToken", handoffToken)
                .build()
                .encode()
                .toUri();
    }

}
