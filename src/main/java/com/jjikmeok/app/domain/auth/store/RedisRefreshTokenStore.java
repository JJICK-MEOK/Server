package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.auth.token.RefreshTokenHasher;
import com.jjikmeok.app.domain.auth.token.SessionEntry;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    public void saveToken(final Long userId, final String refreshToken, final Duration ttl) {
        final SessionEntry sessionEntry = createSessionEntry(userId, refreshToken, ttl);
        stringRedisTemplate.opsForValue().set(generateKey(userId), serialize(sessionEntry), ttl);
    }

    private Optional<SessionEntry> getSession(final Long userId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(generateKey(userId)))
                .map(this::deserialize);
    }

    @Override
    public boolean matches(final Long userId, final String refreshToken) {
        final String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        return getSession(userId)
                .map(SessionEntry::refreshTokenHash)
                .filter(refreshTokenHash::equals)
                .isPresent();
    }

    @Override
    public void deleteToken(final Long userId) {
        stringRedisTemplate.delete(generateKey(userId));
    }

    private SessionEntry createSessionEntry(final Long userId, final String refreshToken, final Duration ttl) {
        return new SessionEntry(
                userId,
                refreshTokenHasher.hash(refreshToken),
                Instant.now(),
                ttl
        );
    }

    private String serialize(final SessionEntry sessionEntry) {
        try {
            return objectMapper.writeValueAsString(sessionEntry);
        } catch (final JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private SessionEntry deserialize(final String value) {
        try {
            return objectMapper.readValue(value, SessionEntry.class);
        } catch (final JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateKey(final Long userId) {
        return KEY_PREFIX + userId;
    }
}
