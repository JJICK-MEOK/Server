package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.personalization.entity.ActivityPreferenceVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityPreferenceVectorRepository extends JpaRepository<ActivityPreferenceVector, Long> {

    Optional<ActivityPreferenceVector> findByActivityId(Long activityId);

    List<ActivityPreferenceVector> findAllByActivityIdIn(List<Long> activityIds);
}
