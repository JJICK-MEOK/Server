package com.jjikmeok.app.domain.onboarding.repository.command;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingTagRepository extends JpaRepository<UserOnboardingTag, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);
}
