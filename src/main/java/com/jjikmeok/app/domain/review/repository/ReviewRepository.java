package com.jjikmeok.app.domain.review.repository;

import com.jjikmeok.app.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByActivityId(Long activityId, Pageable pageable);
    Optional<Review> findByIdAndActivityIdAndUserId(Long id, Long activityId, Long userId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
}
