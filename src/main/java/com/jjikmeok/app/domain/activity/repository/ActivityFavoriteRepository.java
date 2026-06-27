package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.ActivityFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityFavoriteRepository extends JpaRepository<ActivityFavorite, Long> {

    List<ActivityFavorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT f
        FROM ActivityFavorite f
        JOIN FETCH f.activity a
        JOIN FETCH f.user u
        WHERE u.id = :userId
          AND a.recruitEndAt IS NOT NULL
        ORDER BY
          a.recruitEndAt ASC,
          f.createdAt DESC,
          f.id DESC
        """)
    List<ActivityFavorite> findAllByUserIdOrderByRecruitEndAtAsc(@Param("userId") Long userId);

    Optional<ActivityFavorite> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);

    @Query("""
            SELECT f.activity.id
            FROM ActivityFavorite f
            WHERE f.user.id = :userId
              AND f.activity.id IN :activityIds
            """)
    List<Long> findActivityIdsByUserIdAndActivityIdIn(
            @Param("userId") Long userId,
            @Param("activityIds") Collection<Long> activityIds);
}
