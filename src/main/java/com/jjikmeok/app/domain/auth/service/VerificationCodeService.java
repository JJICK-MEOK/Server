package com.jjikmeok.app.domain.auth.service;

import java.security.SecureRandom;
import java.time.Duration;

import com.jjikmeok.app.domain.auth.store.VerificationCodeStore;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;

import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

    /**
     * code 는 최대 6자리수 상한
     */
    private static final int VERIFICATION_CODE_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 코드 발행후, redis 에 저장
     */
    public String issueCode(final VerificationCodeStore store, final String key, final Duration ttl) {
        final String code = generateVerificationCode();
        store.saveCode(key, code, ttl);
        return code;
    }

    /**
     * 코드 검증후, redis 에서 소비
     */
    public void verifyAndConsume(final VerificationCodeStore store, final String key, final String code) {
        final String savedCode = store.getCode(key)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED));

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        store.deleteCode(key);
    }

    /**
     * secureRandom 를 이욯하여, 무작위 6자릿수 코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(VERIFICATION_CODE_BOUND));
    }
}
