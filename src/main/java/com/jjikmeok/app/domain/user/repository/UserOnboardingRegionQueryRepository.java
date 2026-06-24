package com.jjikmeok.app.domain.user.repository;

import com.jjikmeok.app.domain.user.entity.UserOnboardingRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserOnboardingRegionQueryRepository extends JpaRepository<UserOnboardingRegion, Long> {

    @EntityGraph(attributePaths = "region")
    List<UserOnboardingRegion> findAllByUserOnboardingIdOrderByIdAsc(Long userOnboardingId);
}
