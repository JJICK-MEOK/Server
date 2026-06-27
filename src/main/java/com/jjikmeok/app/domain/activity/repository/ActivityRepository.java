package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationCandidateResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.favorite.entity.Favorite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Query("""
            SELECT a.id
            FROM ActivityTag at
            JOIN at.activity a
            WHERE at.tag.id IN :tagIds
              AND a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            GROUP BY a.id, a.createdAt
            ORDER BY COUNT(DISTINCT at.tag.id) DESC, a.createdAt DESC
            """)
    List<Long> findActiveActivityIdsByTagIds(
            @Param("tagIds") List<Long> tagIds,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT new com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationCandidateResponse(
                a.id,
                CASE WHEN COUNT(f.id) > 0 THEN true ELSE false END
            )
            FROM ActivityTag at
            JOIN at.activity a
            JOIN UserOnboardingTag uot ON uot.tag = at.tag
            LEFT JOIN Favorite f ON f.activity = a AND f.user.id = :userId
            WHERE uot.userOnboarding.user.id = :userId
              AND a.isActive = true
              AND a.approvalStatus = :approvalStatus
              AND (a.recruitEndAt IS NULL OR a.recruitEndAt >= :now)
            GROUP BY a.id, a.createdAt
            HAVING COUNT(DISTINCT at.tag.id) >= :minimumMatchedTagCount
            ORDER BY COUNT(DISTINCT at.tag.id) DESC, a.createdAt DESC
            """)
    List<ActivityRecommendationCandidateResponse> findRecommendedActivityCandidatesByUserTags(
            @Param("userId") Long userId,
            @Param("minimumMatchedTagCount") Long minimumMatchedTagCount,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT a
            FROM Activity a
            JOIN FETCH a.region
            LEFT JOIN FETCH a.tags activityTag
            LEFT JOIN FETCH activityTag.tag
            WHERE a.id IN :activityIds
            """)
    List<Activity> findAllByIdInWithSummaryAssociations(@Param("activityIds") List<Long> activityIds);

    default List<Activity> findActiveActivitiesByTagIds(
            List<Long> tagIds,
            ApprovalStatus approvalStatus,
            LocalDateTime now) {
        List<Long> activityIds = findActiveActivityIdsByTagIds(tagIds, approvalStatus, now);
        if (activityIds.isEmpty()) {
            return List.of();
        }

        return sortActivitiesByIds(findAllByIdInWithSummaryAssociations(activityIds), activityIds);
    }

    default List<ActivityRecommendationResponse> findRecommendedActivitiesByUserTags(
            Long userId,
            Long minimumMatchedTagCount,
            ApprovalStatus approvalStatus,
            LocalDateTime now,
            Pageable pageable) {
        List<ActivityRecommendationCandidateResponse> candidates = findRecommendedActivityCandidatesByUserTags(
                userId,
                minimumMatchedTagCount,
                approvalStatus,
                now,
                pageable
        );

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> activityIds = candidates.stream()
                .map(ActivityRecommendationCandidateResponse::activityId)
                .toList();
        List<Activity> activities = sortActivitiesByIds(findAllByIdInWithSummaryAssociations(activityIds), activityIds);
        Map<Long, Boolean> likedByActivityId = new HashMap<>();
        candidates.forEach(candidate -> likedByActivityId.put(candidate.activityId(), candidate.liked()));

        return activities.stream()
                .map(activity -> new ActivityRecommendationResponse(
                        activity,
                        likedByActivityId.getOrDefault(activity.getId(), false)
                ))
                .toList();
    }

    private List<Activity> sortActivitiesByIds(List<Activity> activities, List<Long> activityIds) {
        Map<Long, Integer> orderByActivityId = new HashMap<>();
        for (int i = 0; i < activityIds.size(); i++) {
            orderByActivityId.put(activityIds.get(i), i);
        }

        return activities.stream()
                .sorted(Comparator.comparing(activity -> orderByActivityId.get(activity.getId())))
                .toList();
    }

    boolean existsBySourceTypeAndExternalId(SourceType sourceType, String externalId);

    boolean existsBySourceUrl(String sourceUrl);

    Optional<Activity> findFirstBySourceTypeAndExternalId(SourceType sourceType, String externalId);

    Optional<Activity> findFirstBySourceUrl(String sourceUrl);

    Optional<Activity> findFirstByTitleAndStartAtAndAddress(String title, LocalDateTime startAt, String address);

    Optional<Activity> findFirstByTitleIgnoreCaseAndOrganizerIgnoreCase(String title, String organizer);

    List<Activity> findTop50ByTitleContainingIgnoreCaseOrOrganizerContainingIgnoreCaseOrderByCreatedAtDesc(
            String title,
            String organizer);

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
