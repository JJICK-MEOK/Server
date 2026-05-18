package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingRegionRepository extends JpaRepository<UserOnboardingRegion, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);
}
