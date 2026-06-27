package com.jjikmeok.app.domain.auth.client.kakao;

import com.jjikmeok.app.domain.auth.config.KakaoOAuthProperties;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Validated
@Component
public class KakaoOAuthClient {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final RestClient restClient;
    private final KakaoOAuthProperties kakaoOAuthProperties;

    public KakaoOAuthClient(
            final RestClient.Builder restClientBuilder,
            final KakaoOAuthProperties kakaoOAuthProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.kakaoOAuthProperties = kakaoOAuthProperties;
    }

    public KakaoOAuthRes.TokenResponse getToken(@NotBlank final String code) {
        try {
            final KakaoOAuthRes.TokenResponse tokenResponse = restClient.post()
                    .uri(kakaoOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequestBody(code))
                    .retrieve()
                    .body(KakaoOAuthRes.TokenResponse.class);

            validateTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (final RestClientException e) {
            log.error("Kakao access token request failed.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    public KakaoOAuthRes.UserInfoResponse getUserInfo(@NotBlank final String accessToken) {
        try {
            final KakaoOAuthRes.UserInfoResponse userInfo = restClient.get()
                    .uri(kakaoOAuthProperties.getUserInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoOAuthRes.UserInfoResponse.class);

            validateUserInfoResponse(userInfo);
            return userInfo;
        } catch (final RestClientException e) {
            log.error("Kakao user info request failed.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateTokenResponse(final KakaoOAuthRes.TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateUserInfoResponse(final KakaoOAuthRes.UserInfoResponse userInfo) {
        if (userInfo == null || userInfo.id() == null) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private MultiValueMap<String, String> createTokenRequestBody(final String code) {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        body.add("client_id", kakaoOAuthProperties.getClientId());
        if (StringUtils.hasText(kakaoOAuthProperties.getClientSecret())) {
            body.add("client_secret", kakaoOAuthProperties.getClientSecret());
        }
        body.add("redirect_uri", kakaoOAuthProperties.getRedirectUri());
        body.add("code", code);
        return body;
    }
}
