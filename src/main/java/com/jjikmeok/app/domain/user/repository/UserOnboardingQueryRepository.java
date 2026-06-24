package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOnboardingQueryRepository extends JpaRepository<UserOnboarding, Long> {

    Optional<UserOnboarding> findByUserId(Long userId);
}
