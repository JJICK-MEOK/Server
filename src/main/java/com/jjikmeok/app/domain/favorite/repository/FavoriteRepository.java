package com.jjikmeok.app.domain.favorite.repository;

import com.jjikmeok.app.domain.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT f
        FROM Favorite f
        JOIN FETCH f.activity a
        JOIN FETCH f.user u
        WHERE u.id = :userId
          AND a.recruitEndAt IS NOT NULL
        ORDER BY
          a.recruitEndAt ASC,
          f.createdAt DESC,
          f.id DESC
        """)
    List<Favorite> findAllByUserIdOrderByRecruitEndAtAsc(@Param("userId") Long userId);

    Optional<Favorite> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);

    @Query("""
            SELECT f.activity.id
            FROM Favorite f
            WHERE f.user.id = :userId
              AND f.activity.id IN :activityIds
            """)
    List<Long> findActivityIdsByUserIdAndActivityIdIn(
            @Param("userId") Long userId,
            @Param("activityIds") Collection<Long> activityIds);
}
