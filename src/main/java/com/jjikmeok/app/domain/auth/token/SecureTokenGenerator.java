package com.jjikmeok.app.domain.auth.token;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * state, handoff token, resetToken 등에 사용할 예측 불가능한 URL-safe 토큰을 생성한다.
 */
@Component
public class SecureTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateUrlSafeToken(final int byteLength) {
        final byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
