package com.jjikmeok.app.domain.onboarding.repository.command;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingRegionRepository extends JpaRepository<UserOnboardingRegion, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);
}
