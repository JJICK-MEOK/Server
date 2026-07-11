package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.dto.response.UserProfileTagProjection;
import com.jjikmeok.app.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);

    boolean existsByNickname(String nickname);

    Optional<UserProfile> findByUserId(Long userId);

    @Query("""
            SELECT
                tag.id AS id,
                tag.name AS name,
                tag.type AS type,
                tag.tagGroupType AS tagGroupType
            FROM UserOnboardingTag userOnboardingTag
            JOIN userOnboardingTag.tag tag
            WHERE userOnboardingTag.userOnboarding.user.id = :userId
            ORDER BY userOnboardingTag.id ASC
            """)
    List<UserProfileTagProjection> findTagProjectionsByUserId(@Param("userId") Long userId);
}
