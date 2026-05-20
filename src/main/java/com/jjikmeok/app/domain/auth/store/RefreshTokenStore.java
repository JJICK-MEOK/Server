package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;

public interface RefreshTokenStore {

    void saveToken(Long userId, String refreshToken, Duration ttl);

    boolean matches(Long userId, String refreshToken);

    void deleteToken(Long userId);
}
