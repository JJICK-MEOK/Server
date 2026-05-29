package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserOnboardingRegionRepository extends JpaRepository<UserOnboardingRegion, Long> {

    void deleteAllByUserOnboardingId(Long userOnboardingId);

    @Query("""
            SELECT userOnboardingRegion.region.id
            FROM UserOnboardingRegion userOnboardingRegion
            WHERE userOnboardingRegion.userOnboarding.user.id = :userId
            """)
    List<Long> findRegionIdsByUserId(@Param("userId") Long userId);
}
