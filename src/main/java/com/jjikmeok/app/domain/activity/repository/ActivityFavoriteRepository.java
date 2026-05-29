package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.ActivityFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityFavoriteRepository extends JpaRepository<ActivityFavorite, Long> {

    List<ActivityFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ActivityFavorite> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
}
