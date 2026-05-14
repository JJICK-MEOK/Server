package com.jjikmeok.app.domain.auth.service;

import java.util.Locale;

import com.jjikmeok.app.domain.user.entity.UserRole;
import com.jjikmeok.app.global.security.jwt.JwtProperties;

public final class AuthUtils {

    private AuthUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String resolveRole(final UserRole role) {
        return role.name();
    }

    public static int accessTokenExpiresInSeconds(final JwtProperties jwtProperties) {
        return Math.toIntExact(jwtProperties.getAccessTokenExpirationMs() / 1000);
    }
}
