package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private static final String KEY_PREFIX = "email-verification:code:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveCode(final String email, final String code, final Duration ttl) {
        stringRedisTemplate.opsForValue().set(generateKey(email), code, ttl);
    }

    @Override
    public Optional<String> getCode(final String email) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(generateKey(email)));
    }

    @Override
    public void deleteCode(final String email) {
        stringRedisTemplate.delete(generateKey(email));
    }

    private String generateKey(final String email) {
        return KEY_PREFIX + email;
    }
}
