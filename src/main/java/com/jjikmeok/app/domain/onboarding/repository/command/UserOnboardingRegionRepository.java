package com.jjikmeok.app.domain.onboarding.repository.command;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOnboardingRegionRepository extends JpaRepository<UserOnboardingRegion, Long> {

    @Modifying
    @Query("""
            DELETE FROM UserOnboardingRegion userOnboardingRegion
            WHERE userOnboardingRegion.userOnboarding.id = :userOnboardingId
            """)
    void deleteAllByUserOnboardingId(@Param("userOnboardingId") Long userOnboardingId);
}
