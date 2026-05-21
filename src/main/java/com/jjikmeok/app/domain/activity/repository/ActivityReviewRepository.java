package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.ActivityReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityReviewRepository extends JpaRepository<ActivityReview, Long> {
    List<ActivityReview> findAllByActivityIdOrderByCreatedAtDesc(Long activityId);
    Optional<ActivityReview> findByIdAndActivityIdAndUserId(Long id, Long activityId, Long userId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
}
