package com.jjikmeok.app.domain.auth.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class KakaoOAuthRes {

    private KakaoOAuthRes() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token")
            String accessToken,
            @JsonProperty("refresh_token")
            String refreshToken,
            @JsonProperty("token_type")
            String tokenType,
            @JsonProperty("expires_in")
            long expiresIn,
            String scope
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfoResponse(
            Long id,
            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String nickname,
            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {
    }
}
