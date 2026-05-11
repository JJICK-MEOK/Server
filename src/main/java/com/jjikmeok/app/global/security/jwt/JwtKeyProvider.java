package com.jjikmeok.app.global.security.jwt;

import javax.crypto.SecretKey;

import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.security.exception.JwtTokenException;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtKeyProvider {

    private final SecretKey signingKey;

    public JwtKeyProvider(JwtProperties jwtProperties) {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException | DecodingException | WeakKeyException e) {
            throw new JwtTokenException(ErrorCode.JWT_INVALID_SECRET);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "JWT 인증키가 생성되었습니다. (algorithm=HS256, keyLength={} bytes)",
                    keyBytes.length
            );
        }
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}


