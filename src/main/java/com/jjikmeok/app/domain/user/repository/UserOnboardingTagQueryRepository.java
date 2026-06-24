package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOnboardingTagQueryRepository extends JpaRepository<UserOnboardingTag, Long> {

    @EntityGraph(attributePaths = "tag")
    List<UserOnboardingTag> findAllByUserOnboardingIdOrderByIdAsc(Long userOnboardingId);

    @Query("""
            SELECT userOnboardingTag.tag.id
            FROM UserOnboardingTag userOnboardingTag
            WHERE userOnboardingTag.userOnboarding.user.id = :userId
            """)
    List<Long> findTagIdsByUserId(@Param("userId") Long userId);
}
