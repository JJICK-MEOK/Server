package com.jjikmeok.app.domain.image.repository;

import com.jjikmeok.app.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findAllByActivityIdOrderBySortOrderAscIdAsc(Long activityId);

    Optional<Image> findByIdAndActivityId(Long id, Long activityId);

    boolean existsByActivityIdAndSortOrder(Long activityId, Integer sortOrder);

    boolean existsByActivityIdAndSortOrderAndIdNot(Long activityId, Integer sortOrder, Long id);
}
