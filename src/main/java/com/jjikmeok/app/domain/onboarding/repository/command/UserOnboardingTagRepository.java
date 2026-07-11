package com.jjikmeok.app.domain.onboarding.repository.command;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOnboardingTagRepository extends JpaRepository<UserOnboardingTag, Long> {

    @Modifying
    @Query("""
            DELETE FROM UserOnboardingTag userOnboardingTag
            WHERE userOnboardingTag.userOnboarding.id = :userOnboardingId
            """)
    void deleteAllByUserOnboardingId(@Param("userOnboardingId") Long userOnboardingId);
}
