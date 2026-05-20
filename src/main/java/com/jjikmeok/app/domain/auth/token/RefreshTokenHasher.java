package com.jjikmeok.app.domain.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;

@Component
public class RefreshTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    public String hash(final String refreshToken) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            final byte[] hashBytes = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
