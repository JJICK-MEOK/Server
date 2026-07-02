package com.jjikmeok.app.domain.auth.store;

import java.time.Duration;
import java.util.Optional;

public interface PasswordResetTokenStore {

    void save(String resetToken, String email, Duration ttl);

    Optional<String> consume(String resetToken);
}
