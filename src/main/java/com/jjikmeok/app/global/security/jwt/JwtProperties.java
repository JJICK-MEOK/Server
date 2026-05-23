package com.jjikmeok.app.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@ConfigurationProperties(prefix = "jwt")
@Getter
@RequiredArgsConstructor
public class JwtProperties {

    @NotBlank
    private final String secret;
    @Positive
    private final long accessTokenExpirationMs;
    @Positive
    private final long refreshTokenExpirationMs;
}