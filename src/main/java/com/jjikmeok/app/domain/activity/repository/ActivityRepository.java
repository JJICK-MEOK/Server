package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Activity a
            SET a.viewCount = a.viewCount + 1
            WHERE a.id = :id
              AND a.isActive = true
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            """)
    int incrementViewCount(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Activity a
            SET a.viewCount = a.viewCount + 1
            WHERE a.id = :id
              AND a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            """)
    int incrementApprovedViewCount(
            @Param("id") Long id,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Activity a SET a.isActive = false WHERE a.isActive = true AND a.endAt < :now")
    int deactivateEnded(@Param("now") LocalDateTime now);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
              AND (:regionId IS NULL OR a.region.id = :regionId)
              AND (:category IS NULL OR a.category = :category)
              AND (:type IS NULL OR a.activityType = :type)
              AND (:keyword IS NULL OR a.title LIKE %:keyword% OR a.description LIKE %:keyword%)
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findActiveActivitiesByFilters(
            @Param("regionId") Long regionId,
            @Param("category") com.jjikmeok.app.domain.activity.enums.ActivityCategory category,
            @Param("type") com.jjikmeok.app.domain.activity.enums.ActivityType type,
            @Param("keyword") String keyword,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
              AND (:category IS NULL OR a.category = :category)
              AND (:type IS NULL OR a.activityType = :type)
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findApprovedActivitiesByFilters(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("category") ActivityCategory category,
            @Param("type") ActivityType type,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT COUNT(a)
            FROM Activity a
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
              AND (:category IS NULL OR a.category = :category)
              AND (:type IS NULL OR a.activityType = :type)
            """)
    long countApprovedActivitiesByFilters(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("category") ActivityCategory category,
            @Param("type") ActivityType type,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            LEFT JOIN a.tags activityTag
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
              AND activityTag.tag.id IN :tagIds
            ORDER BY a.recruitEndAt ASC, a.createdAt DESC
            """)
    List<Activity> findRecommendedByPreferenceTagIds(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("tagIds") List<Long> tagIds,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
              AND a.region.id IN :regionIds
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findRecommendedByRegionIds(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("regionIds") List<Long> regionIds,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findApprovedLatest(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND a.recruitEndAt >= :now
            ORDER BY a.recruitEndAt ASC, a.viewCount DESC, a.likeCount DESC
            """)
    List<Activity> findApprovedClosingSoon(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            ORDER BY a.viewCount DESC, a.likeCount DESC, a.createdAt DESC
            """)
    List<Activity> findApprovedPopular(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findActiveActivitiesWithRegion(@Param("now") LocalDateTime now);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.region.id = :regionId
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findActiveActivitiesByRegionIdWithRegion(
            @Param("regionId") Long regionId,
            @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Activity a JOIN FETCH a.region WHERE a.id = :id")
    Optional<Activity> findByIdWithRegion(@Param("id") Long id);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.id = :id
              AND a.isActive = true
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            """)
    Optional<Activity> findOpenByIdWithRegion(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.id = :id
              AND a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            """)
    Optional<Activity> findApprovedByIdWithRegion(
            @Param("id") Long id,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now);

    boolean existsBySourceTypeAndExternalId(SourceType sourceType, String externalId);

    boolean existsBySourceUrl(String sourceUrl);

    Optional<Activity> findFirstBySourceTypeAndExternalId(SourceType sourceType, String externalId);

    Optional<Activity> findFirstBySourceUrl(String sourceUrl);

    Optional<Activity> findFirstByTitleAndStartAtAndAddress(String title, LocalDateTime startAt, String address);

    default Optional<Activity> findDuplicate(SourceType sourceType, String externalId, String sourceUrl,
                                             String title, LocalDateTime startAt, String address) {
        if (externalId != null && !externalId.isBlank()) {
            Optional<Activity> activity = findFirstBySourceTypeAndExternalId(sourceType, externalId);
            if (activity.isPresent()) return activity;
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            Optional<Activity> activity = findFirstBySourceUrl(sourceUrl);
            if (activity.isPresent()) return activity;
        }
        if (title != null && !title.isBlank() && startAt != null && address != null && !address.isBlank()) {
            return findFirstByTitleAndStartAtAndAddress(title, startAt, address);
        }
        return Optional.empty();
    }

    boolean existsByRegionId(Long regionId);
}
