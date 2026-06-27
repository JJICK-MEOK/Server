package com.jjikmeok.app.domain.auth.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "oauth2.kakao")
public class KakaoOAuthProperties {

    @NotBlank
    private String clientId;

    private String clientSecret;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String authorizationUri;

    @NotBlank
    private String tokenUri;

    @NotBlank
    private String userInfoUri;

    @NotEmpty
    private List<String> scopes = List.of("account_email", "profile_nickname");

    private Duration stateTtl = Duration.ofMinutes(5);

    private Duration handoffTtl = Duration.ofMinutes(5);

    @NotBlank
    private String appDeepLinkUri;

    @Positive
    private int stateTokenBytes = 32;

    @Positive
    private int handoffTokenBytes = 32;
}
