package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.ActivityImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityImageRepository extends JpaRepository<ActivityImage, Long> {

    List<ActivityImage> findAllByActivityIdOrderBySortOrderAscIdAsc(Long activityId);

    Optional<ActivityImage> findByIdAndActivityId(Long id, Long activityId);

    boolean existsByActivityIdAndSortOrder(Long activityId, Integer sortOrder);

    boolean existsByActivityIdAndSortOrderAndIdNot(Long activityId, Integer sortOrder, Long id);
}
