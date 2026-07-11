package com.jjikmeok.app.domain.onboarding.repository.command;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    Optional<UserOnboarding> findByUserId(Long userId);
}
