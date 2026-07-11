package com.jjikmeok.app.domain.onboarding.repository.query;

import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOnboardingRegionQueryRepository extends JpaRepository<UserOnboardingRegion, Long> {

    @EntityGraph(attributePaths = "region")
    List<UserOnboardingRegion> findAllByUserOnboardingIdOrderByIdAsc(Long userOnboardingId);

    @Query("""
            SELECT userOnboardingRegion.region.id
            FROM UserOnboardingRegion userOnboardingRegion
            WHERE userOnboardingRegion.userOnboarding.user.id = :userId
            """)
    List<Long> findRegionIdsByUserId(@Param("userId") Long userId);
}
