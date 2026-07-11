package com.jjikmeok.app.domain.onboarding.repository.query;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOnboardingTagQueryRepository extends JpaRepository<UserOnboardingTag, Long> {

    @EntityGraph(attributePaths = "tag")
    List<UserOnboardingTag> findAllByUserOnboardingIdOrderByIdAsc(Long userOnboardingId);

    @Query("""
            SELECT userOnboardingTag
            FROM UserOnboardingTag userOnboardingTag
            JOIN FETCH userOnboardingTag.tag
            WHERE userOnboardingTag.userOnboarding.user.id = :userId
            """)
    List<UserOnboardingTag> findAllByUserIdWithTag(@Param("userId") Long userId);

    @Query("""
            SELECT userOnboardingTag.tag.id
            FROM UserOnboardingTag userOnboardingTag
            WHERE userOnboardingTag.userOnboarding.user.id = :userId
            """)
    List<Long> findTagIdsByUserId(@Param("userId") Long userId);
}
