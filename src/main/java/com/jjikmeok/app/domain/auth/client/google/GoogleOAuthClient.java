package com.jjikmeok.app.domain.auth.client.google;

import com.jjikmeok.app.domain.auth.config.GoogleOAuthProperties;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Validated
@Component
public class GoogleOAuthClient {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final RestClient restClient;
    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleOAuthClient(
            final RestClient.Builder restClientBuilder,
            final GoogleOAuthProperties googleOAuthProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.googleOAuthProperties = googleOAuthProperties;
    }

    public GoogleOAuthRes.TokenResponse getToken(@NotBlank final String code) {
        try {
            final GoogleOAuthRes.TokenResponse tokenResponse = restClient.post()
                    .uri(googleOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequestBody(code))
                    .retrieve()
                    .body(GoogleOAuthRes.TokenResponse.class);

            validateTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (final RestClientException e) {
            log.error("구글 액세스 토큰 요청에 실패했습니다.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    public GoogleOAuthRes.UserInfoResponse getUserInfo(@NotBlank final String accessToken) {
        try {
            final GoogleOAuthRes.UserInfoResponse userInfo = restClient.get()
                    .uri(googleOAuthProperties.getUserInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GoogleOAuthRes.UserInfoResponse.class);

            validateUserInfoResponse(userInfo);
            return userInfo;
        } catch (final RestClientException e) {
            log.error("구글 사용자 정보 조회에 실패했습니다.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateTokenResponse(final GoogleOAuthRes.TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateUserInfoResponse(final GoogleOAuthRes.UserInfoResponse userInfo) {
        if (userInfo == null || userInfo.sub() == null || userInfo.sub().isBlank()) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private MultiValueMap<String, String> createTokenRequestBody(final String code) {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        body.add("client_id", googleOAuthProperties.getClientId());
        body.add("client_secret", googleOAuthProperties.getClientSecret());
        body.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        body.add("code", code);
        return body;
    }
}
