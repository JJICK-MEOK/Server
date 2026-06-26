package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;

/**
 * OAuth CSRF 방지 state 값을 저장하고 1회용으로 소비하는 저장소
 */
public interface OAuthStateStore {

    void save(String state, Duration ttl);

    boolean consume(String state);
}
