package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis를 사용해 OAuth state 값을 저장하고 원자적으로 소비
 */
@Component
@RequiredArgsConstructor
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final String KEY_PREFIX = "oauth:state:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(final String state, final String value, final Duration ttl) {
        stringRedisTemplate.opsForValue().set(generateKey(state), value, ttl);
    }

    @Override
    public String consumeAndGet(final String state) {
        return stringRedisTemplate.opsForValue().getAndDelete(generateKey(state));
    }

    private String generateKey(final String state) {
        return KEY_PREFIX + state;
    }
}
