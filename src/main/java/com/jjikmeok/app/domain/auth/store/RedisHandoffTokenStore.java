package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis를 사용해 handoff token을 저장하고 원자적으로 소비
 */
@Component
@RequiredArgsConstructor
public class RedisHandoffTokenStore implements HandoffTokenStore {

    private static final String KEY_PREFIX = "handoff:";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void save(final String token, final HandoffTokenEntry entry, final Duration ttl) {
        stringRedisTemplate.opsForValue().set(generateKey(token), serialize(entry), ttl);
    }

    @Override
    public Optional<HandoffTokenEntry> consume(final String token) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().getAndDelete(generateKey(token)))
                .map(this::deserialize);
    }

    private String serialize(final HandoffTokenEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (final JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private HandoffTokenEntry deserialize(final String value) {
        try {
            return objectMapper.readValue(value, HandoffTokenEntry.class);
        } catch (final JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redis key를 생성합니다.
     *
     * @param token handoff token
     * @return Redis key
     */
    private String generateKey(final String token) {
        return KEY_PREFIX + token;
    }
}
