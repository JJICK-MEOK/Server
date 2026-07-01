package com.jjikmeok.app.domain.auth.service;

import java.net.URI;

import com.jjikmeok.app.domain.auth.client.naver.NaverOAuthClient;
import com.jjikmeok.app.domain.auth.client.naver.NaverOAuthRes;
import com.jjikmeok.app.domain.auth.config.NaverOAuthProperties;
import com.jjikmeok.app.domain.auth.store.OAuthStateStore;
import com.jjikmeok.app.domain.auth.token.OAuthTokenGenerator;
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
public class NaverOAuthHandoffService {

    private final NaverOAuthClient naverOAuthClient;
    private final NaverOAuthProperties naverOAuthProperties;
    private final OAuthStateStore oAuthStateStore;
    private final OAuthTokenGenerator oAuthTokenGenerator;
    private final OAuthHandoffCommonService oAuthHandoffCommonService;

    public URI createNaverLoginUri() {
        final String state = oAuthTokenGenerator.generateUrlSafeToken(naverOAuthProperties.getStateTokenBytes());
        oAuthStateStore.save(state, naverOAuthProperties.getStateTtl());

        log.debug("네이버 OAuth 로그인 URL 생성 완료.");
        return UriComponentsBuilder.fromUriString(naverOAuthProperties.getAuthorizationUri())
                .queryParam("client_id", naverOAuthProperties.getClientId())
                .queryParam("redirect_uri", naverOAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    @Transactional
    public URI handleNaverCallback(final String code, final String state, final String error) {
        validateCallbackError(error);
        validateState(state);
        validateCode(code);

        final NaverOAuthRes.TokenResponse tokenResponse = naverOAuthClient.getToken(code, state);
        final NaverOAuthRes.UserInfoResponse userInfo = naverOAuthClient.getUserInfo(tokenResponse.accessToken());
        final OAuthUserResult userResult = findOrCreateUser(userInfo);
        final String handoffToken = oAuthHandoffCommonService.createHandoffToken(
                userResult,
                naverOAuthProperties.getHandoffTokenBytes(),
                naverOAuthProperties.getHandoffTtl()
        );

        log.debug("네이버 OAuth 콜백 처리 완료. userId={}, newMember={}",
                userResult.user().getId(), userResult.newMember());
        return createAppDeepLinkUri(handoffToken);
    }

    private void validateCallbackError(final String error) {
        if (error == null || error.isBlank()) {
            return;
        }
        log.warn("네이버 OAuth 콜백 실패 - OAuth 제공자 오류가 전달되었습니다. error={}", error);
        throw new CustomException(ErrorCode.AUTH_NAVER_CALLBACK_FAILED);
    }

    private void validateState(final String state) {
        if (state == null || state.isBlank() || !oAuthStateStore.consume(state)) {
            log.warn("네이버 OAuth 콜백 실패 - 유효하지 않은 state입니다.");
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }
    }

    private void validateCode(final String code) {
        if (code == null || code.isBlank()) {
            log.warn("네이버 OAuth 콜백 실패 - 인가 코드가 없습니다.");
            throw new CustomException(ErrorCode.AUTH_NAVER_CALLBACK_FAILED);
        }
    }

    private OAuthUserResult findOrCreateUser(final NaverOAuthRes.UserInfoResponse userInfo) {
        final NaverOAuthRes.Profile profile = userInfo.response();
        return oAuthHandoffCommonService.findOrCreateUser(
                AuthProvider.NAVER,
                profile.id(),
                profile.email()
        );
    }

    private URI createAppDeepLinkUri(final String handoffToken) {
        return UriComponentsBuilder.fromUriString(naverOAuthProperties.getAppDeepLinkUri())
                .queryParam("handoffToken", handoffToken)
                .build()
                .encode()
                .toUri();
    }
}
