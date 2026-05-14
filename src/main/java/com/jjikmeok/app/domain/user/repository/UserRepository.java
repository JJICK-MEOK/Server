package com.jjikmeok.app.domain.user.repository;


import java.util.Optional;

import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
}
