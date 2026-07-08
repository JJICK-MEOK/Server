package com.jjikmeok.app.domain.favorite.repository;

import com.jjikmeok.app.domain.favorite.entity.Favorite;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @Query("""
            SELECT DISTINCT f
            FROM Favorite f
            JOIN FETCH f.user u
            JOIN FETCH f.activity a
            JOIN FETCH a.region
            LEFT JOIN FETCH a.tags activityTag
            LEFT JOIN FETCH activityTag.tag
            WHERE u.id = :userId
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT f
            FROM Favorite f
            JOIN FETCH f.user u
            JOIN FETCH f.activity a
            JOIN FETCH a.region
            LEFT JOIN FETCH a.tags activityTag
            LEFT JOIN FETCH activityTag.tag
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

    @Query("""
        SELECT DISTINCT f
        FROM Favorite f
        JOIN FETCH f.activity a
        JOIN FETCH a.region
        LEFT JOIN FETCH a.tags activityTag
        LEFT JOIN FETCH activityTag.tag
        WHERE f.user.id = :userId
          AND a.isActive = true
          AND a.approvalStatus = :approvalStatus
          AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
        ORDER BY f.createdAt DESC, f.id DESC
        """)
    List<Favorite> findPageFavoritesOrderBySavedDesc(
            @Param("userId") Long userId,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now);

    @Query("""
        SELECT DISTINCT f
        FROM Favorite f
        JOIN FETCH f.activity a
        JOIN FETCH a.region
        LEFT JOIN FETCH a.tags activityTag
        LEFT JOIN FETCH activityTag.tag
        WHERE f.user.id = :userId
          AND a.isActive = true
          AND a.approvalStatus = :approvalStatus
          AND a.recruitEndAt IS NOT NULL
          AND a.recruitEndAt >= :now
        ORDER BY a.recruitEndAt ASC, f.createdAt DESC, f.id DESC
        """)
    List<Favorite> findPageFavoritesOrderByDeadlineAsc(
            @Param("userId") Long userId,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now);

}
