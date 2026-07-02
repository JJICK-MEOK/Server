package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisPasswordResetTokenStore implements PasswordResetTokenStore {

    private static final String KEY_PREFIX = "password-reset:token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(final String resetToken, final String email, final Duration ttl) {
        stringRedisTemplate.opsForValue().set(generateKey(resetToken), email, ttl);
    }

    @Override
    public Optional<String> consume(final String resetToken) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().getAndDelete(generateKey(resetToken)));
    }

    private String generateKey(final String resetToken) {
        return KEY_PREFIX + resetToken;
    }
}
