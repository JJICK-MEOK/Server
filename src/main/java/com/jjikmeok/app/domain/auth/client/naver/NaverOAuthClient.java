package com.jjikmeok.app.domain.auth.client.naver;

import com.jjikmeok.app.domain.auth.config.NaverOAuthProperties;
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
public class NaverOAuthClient {

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final RestClient restClient;
    private final NaverOAuthProperties naverOAuthProperties;

    public NaverOAuthClient(
            final RestClient.Builder restClientBuilder,
            final NaverOAuthProperties naverOAuthProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.naverOAuthProperties = naverOAuthProperties;
    }

    public NaverOAuthRes.TokenResponse getToken(@NotBlank final String code, @NotBlank final String state) {
        try {
            final NaverOAuthRes.TokenResponse tokenResponse = restClient.post()
                    .uri(naverOAuthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequestBody(code, state))
                    .retrieve()
                    .body(NaverOAuthRes.TokenResponse.class);

            validateTokenResponse(tokenResponse);
            return tokenResponse;
        } catch (final RestClientException e) {
            log.error("네이버 액세스 토큰 요청에 실패했습니다.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    public NaverOAuthRes.UserInfoResponse getUserInfo(@NotBlank final String accessToken) {
        try {
            final NaverOAuthRes.UserInfoResponse userInfo = restClient.get()
                    .uri(naverOAuthProperties.getUserInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(NaverOAuthRes.UserInfoResponse.class);

            validateUserInfoResponse(userInfo);
            return userInfo;
        } catch (final RestClientException e) {
            log.error("네이버 사용자 정보 조회에 실패했습니다.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateTokenResponse(final NaverOAuthRes.TokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private void validateUserInfoResponse(final NaverOAuthRes.UserInfoResponse userInfo) {
        if (userInfo == null || userInfo.response() == null
                || userInfo.response().id() == null || userInfo.response().id().isBlank()) {
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    private MultiValueMap<String, String> createTokenRequestBody(final String code, final String state) {
        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE_AUTHORIZATION_CODE);
        body.add("client_id", naverOAuthProperties.getClientId());
        body.add("client_secret", naverOAuthProperties.getClientSecret());
        body.add("redirect_uri", naverOAuthProperties.getRedirectUri());
        body.add("code", code);
        body.add("state", state);
        return body;
    }
}
