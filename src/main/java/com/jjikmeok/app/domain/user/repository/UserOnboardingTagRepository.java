package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOnboardingTagRepository extends JpaRepository<UserOnboardingTag, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);

    @Query("""
            SELECT userOnboardingTag
            FROM UserOnboardingTag userOnboardingTag
            JOIN FETCH userOnboardingTag.tag
            WHERE userOnboardingTag.userOnboarding.user.id = :userId
            """)
    List<UserOnboardingTag> findAllByUserIdWithTag(@Param("userId") Long userId);
}
