package com.jjikmeok.app.domain.auth.client.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class NaverOAuthRes {

    private NaverOAuthRes() {
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
            long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfoResponse(
            @JsonProperty("resultcode")
            String resultCode,
            String message,
            Profile response
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String id,
            String email,
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) {
    }
}
