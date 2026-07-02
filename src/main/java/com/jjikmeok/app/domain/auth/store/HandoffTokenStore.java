package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.util.Optional;

import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;

public interface HandoffTokenStore {

    void save(String token, HandoffTokenEntry entry, Duration ttl);

    Optional<HandoffTokenEntry> consume(String token);
}
