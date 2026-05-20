package com.jjikmeok.app.domain.auth.token;

import java.time.Duration;
import java.time.Instant;

public record SessionEntry(
        Long userId,
        String refreshTokenHash,
        Instant lastActivityAt,
        Duration ttl
) {
}
