package com.jjikmeok.app.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@ConfigurationProperties(prefix = "jwt")
@Getter
@RequiredArgsConstructor
public class JwtProperties {

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
}