package com.jjikmeok.app.global.security.jwt;

import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.security.exception.JwtTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;
    private final JwtKeyProvider jwtKeyProvider;

    public String createAccessToken(final Long userId, final String role) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .claim(CLAIM_ROLE, role)
                .signWith(jwtKeyProvider.getSigningKey());

        return builder.compact();
    }

    public String createRefreshToken(final Long userId) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())
                .signWith(jwtKeyProvider.getSigningKey())
                .compact();
    }

    public void validateToken(final String token) {
        verifyAndParse(token);
    }

    public Claims parseClaims(final String token) {
        return verifyAndParse(token);
    }

    public String getSubject(final String token) {
        final String subject = parseClaims(token).getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_SUBJECT);
        }
        return subject;
    }

    public Long getUserId(final String token) {
        try {
            return Long.valueOf(getSubject(token));
        } catch (final NumberFormatException e) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_SUBJECT, e);
        }
    }

    public String getRole(final String token) {
        final String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        if (role == null || role.isBlank()) {
            throw new JwtTokenException(ErrorCode.JWT_MISSING_ROLE_CLAIM);
        }
        return role;
    }

    public String getJti(final String token) {
        final String jti = parseClaims(token).getId();
        if (jti == null || jti.isBlank()) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_TOKEN);
        }
        return jti;
    }

    private String requireToken(final String token) {
        if (token == null || token.isBlank()) {
            throw new JwtTokenException(ErrorCode.JWT_EMPTY_TOKEN);
        }
        return token.trim();
    }

    private Claims verifyAndParse(final String token) {
        final String normalizedToken = requireToken(token);

        try {
            return Jwts.parser()
                    .verifyWith(jwtKeyProvider.getSigningKey())
                    .build()
                    .parseSignedClaims(normalizedToken)
                    .getPayload();
        } catch (final ExpiredJwtException e) {
            throw new JwtTokenException(ErrorCode.JWT_EXPIRED_TOKEN, e);
        } catch (final SecurityException e) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_SIGNATURE, e);
        } catch (final MalformedJwtException e) {
            throw new JwtTokenException(ErrorCode.JWT_MALFORMED_TOKEN, e);
        } catch (final UnsupportedJwtException e) {
            throw new JwtTokenException(ErrorCode.JWT_UNSUPPORTED_TOKEN, e);
        } catch (final JwtException | IllegalArgumentException e) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_TOKEN, e);
        }
    }
}
