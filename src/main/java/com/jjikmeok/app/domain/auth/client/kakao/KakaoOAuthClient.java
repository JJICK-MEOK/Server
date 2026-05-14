package com.jjikmeok.app.domain.auth.client.kakao;

import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class KakaoOAuthClient {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final RestClient restClient;
    private final String clientId;
    private final String redirectUri;

    public KakaoOAuthClient(
            final RestClient.Builder restClientBuilder,
            @Value("${oauth2.kakao.client-id}") final String clientId,
            @Value("${oauth2.kakao.redirect-uri}") final String redirectUri
    ) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public String getAccessToken(@NotBlank final String code) {
        try {
            final KakaoOAuthRes.TokenResponse tokenResponse = restClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequestBody(code))
                    .retrieve()
                    .body(KakaoOAuthRes.TokenResponse.class);

            validateTokenResponse(tokenResponse);
            return tokenResponse.accessToken();
        } catch (final RestClientException e) {
            log.error("카카오 액세스 토큰 요청에 실패했습니다.", e);
            throw new CustomException(ErrorCode.AUTH_INVALID_SOCIAL_ACCESS_TOKEN);
        }
    }

    public KakaoOAuthRes.UserInfoResponse getUserInfo(@NotBlank final String accessToken) {
        try {
            final KakaoOAuthRes.UserInfoResponse userInfo = restClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoOAuthRes.UserInfoResponse.class);

            validateUserInfoResponse(userInfo);
            return userInfo;
        } catch (final RestClientException e) {
            log.error("카카오 사용자 정보 조회에 실패했습니다.", e);
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
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        return body;
    }
}
