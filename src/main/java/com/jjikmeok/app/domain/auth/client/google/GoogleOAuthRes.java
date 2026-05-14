package com.jjikmeok.app.domain.auth.client.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class GoogleOAuthRes {

    private GoogleOAuthRes() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token")
            String accessToken,
            @JsonProperty("id_token")
            String idToken,
            @JsonProperty("expires_in")
            long expiresIn,
            String scope
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserInfoResponse(
            String sub,
            String email,
            String picture
    ) {
    }
}
