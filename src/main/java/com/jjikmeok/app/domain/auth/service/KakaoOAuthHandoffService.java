package com.jjikmeok.app.domain.auth.service;

import java.net.URI;
import java.time.Instant;
import java.util.StringJoiner;

import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthClient;
import com.jjikmeok.app.domain.auth.client.kakao.KakaoOAuthRes;
import com.jjikmeok.app.domain.auth.config.KakaoOAuthProperties;
import com.jjikmeok.app.domain.auth.store.HandoffTokenStore;
import com.jjikmeok.app.domain.auth.store.OAuthStateStore;
import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;
import com.jjikmeok.app.domain.auth.token.OAuthTokenGenerator;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoOAuthHandoffService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final OAuthStateStore oAuthStateStore;
    private final HandoffTokenStore handoffTokenStore;
    private final OAuthTokenGenerator oAuthTokenGenerator;
    private final UserRepository userRepository;

    public URI createKakaoLoginUri() {
        final String state = oAuthTokenGenerator.generateUrlSafeToken(kakaoOAuthProperties.getStateTokenBytes());
        oAuthStateStore.save(state, kakaoOAuthProperties.getStateTtl());

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
        validateCallbackError(error);
        validateState(state);
        validateCode(code);

        final KakaoOAuthRes.TokenResponse tokenResponse = kakaoOAuthClient.getToken(code);
        final KakaoOAuthRes.UserInfoResponse userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.accessToken());
        final OAuthUserResult userResult = findOrCreateUser(userInfo);
        final String handoffToken = createHandoffToken(userResult);

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
        throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    private void validateState(final String state) {
        if (state == null || state.isBlank() || !oAuthStateStore.consume(state)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_OAUTH_STATE);
        }
    }

    private void validateCode(final String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private OAuthUserResult findOrCreateUser(final KakaoOAuthRes.UserInfoResponse userInfo) {
        final String providerId = String.valueOf(userInfo.id());
        final String email = AuthUtils.normalizeEmail(userInfo.kakaoAccount().email());

        return userRepository.findByAuthProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .map(user -> new OAuthUserResult(user, false))
                .orElseGet(() -> {
                    final User savedUser = userRepository.save(User.createForOAuth2(email, AuthProvider.KAKAO, providerId));
                    log.info("Kakao OAuth handoff signup completed. userId={}, providerId={}", savedUser.getId(), providerId);
                    return new OAuthUserResult(savedUser, true);
                });
    }

    private String createHandoffToken(final OAuthUserResult userResult) {
        final String handoffToken = oAuthTokenGenerator.generateUrlSafeToken(kakaoOAuthProperties.getHandoffTokenBytes());
        final HandoffTokenEntry entry = new HandoffTokenEntry(
                userResult.user().getId(),
                userResult.newMember(),
                Instant.now()
        );
        handoffTokenStore.save(handoffToken, entry, kakaoOAuthProperties.getHandoffTtl());
        return handoffToken;
    }

    private URI createAppDeepLinkUri(final String handoffToken) {
        return UriComponentsBuilder.fromUriString(kakaoOAuthProperties.getAppDeepLinkUri())
                .queryParam("handoffToken", handoffToken)
                .build()
                .encode()
                .toUri();
    }

    private record OAuthUserResult(

            User user,
            boolean newMember
    ) {
    }
}
