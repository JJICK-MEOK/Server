package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingTagRepository extends JpaRepository<UserOnboardingTag, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);
}
