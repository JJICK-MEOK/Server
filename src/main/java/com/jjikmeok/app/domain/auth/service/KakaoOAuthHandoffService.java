package com.jjikmeok.app.domain.auth.service;

import java.net.URI;
import java.util.StringJoiner;

import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthClient;
import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthRes;
import com.jjikmeok.app.domain.auth.config.KakaoOAuthProperties;
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
public class KakaoOAuthHandoffService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final OAuthStateStore oAuthStateStore;
    private final SecureTokenGenerator secureTokenGenerator;
    private final OAuthHandoffCommonService oAuthHandoffCommonService;

    public URI createKakaoLoginUri() {
        final String state = secureTokenGenerator.generateUrlSafeToken(kakaoOAuthProperties.getStateTokenBytes());
        oAuthStateStore.save(state, kakaoOAuthProperties.getStateTtl());

        log.debug("카카오 OAuth 로그인 URL 생성 완료.");
        return UriComponentsBuilder.fromUriString(kakaoOAuthProperties.getAuthorizationUri())
                .queryParam("client_id", kakaoOAuthProperties.getClientId())
                .queryParam("redirect_uri", kakaoOAuthProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", createScopeValue())
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    @Transactional
    public URI handleKakaoCallback(final String code, final String state, final String error) {
        validateState(state);
        validateCallbackError(error);
        validateCode(code);

        final KakaoOAuthRes.TokenResponse tokenResponse = kakaoOAuthClient.getToken(code);
        final KakaoOAuthRes.UserInfoResponse userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.accessToken());
        final OAuthUserResult userResult = findOrCreateUser(userInfo);
        final String handoffToken = oAuthHandoffCommonService.createHandoffToken(
                userResult,
                kakaoOAuthProperties.getHandoffTokenBytes(),
                kakaoOAuthProperties.getHandoffTtl()
        );

        log.debug("카카오 OAuth 콜백 처리 완료. userId={}, newMember={}",
                userResult.user().getId(), userResult.newMember());
        return createAppDeepLinkUri(handoffToken);
    }

    private String createScopeValue() {
        final StringJoiner joiner = new StringJoiner(" ");
        kakaoOAuthProperties.getScopes().forEach(joiner::add);
        return joiner.toString();
    }

    private void validateCallbackError(final String error) {
        if (error == null || error.isBlank()) {
            return;
        }
        log.warn("카카오 OAuth 콜백 실패 - OAuth 제공자 오류가 전달되었습니다. error={}", error);
        throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    private void validateState(final String state) {
        if (state == null || state.isBlank() || !oAuthStateStore.consume(state)) {
            log.warn("카카오 OAuth 콜백 실패 - 유효하지 않은 state입니다.");
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }
    }

    private void validateCode(final String code) {
        if (code == null || code.isBlank()) {
            log.warn("카카오 OAuth 콜백 실패 - 인가 코드가 없습니다.");
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private OAuthUserResult findOrCreateUser(final KakaoOAuthRes.UserInfoResponse userInfo) {
        return oAuthHandoffCommonService.findOrCreateUser(
                AuthProvider.KAKAO,
                String.valueOf(userInfo.id()),
                extractEmail(userInfo)
        );
    }

    private String extractEmail(final KakaoOAuthRes.UserInfoResponse userInfo) {
        if (userInfo.kakaoAccount() == null) {
            return null;
        }
        return userInfo.kakaoAccount().email();
    }

    private URI createAppDeepLinkUri(final String handoffToken) {
        return UriComponentsBuilder.fromUriString(kakaoOAuthProperties.getAppDeepLinkUri())
                .queryParam("handoffToken", handoffToken)
                .build()
                .encode()
                .toUri();
    }

}
