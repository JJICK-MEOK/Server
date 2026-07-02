package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalizationRepository extends JpaRepository<UserOnboardingTag, Long> {
    @Query(
            value = """
                    SELECT t.name
                    FROM user_onboarding uo
                    JOIN user_onboarding_tags uot
                        ON uot.user_onboarding_id = uo.id
                    JOIN tags t
                        ON t.id = uot.tag_id
                    WHERE uo.user_id = :userId
                    """,
            nativeQuery = true
    )
    List<String> findTagNamesByUserId(@Param("userId") Long userId);

    @Query(
            value = """
            WITH uot_result AS (
                SELECT DISTINCT
                    uot.tag_id
                FROM users u
                JOIN user_onboarding uo
                    ON uo.user_id = u.id
                JOIN user_onboarding_tags uot
                    ON uot.user_onboarding_id = uo.id
                WHERE u.id = :userId
            )
            SELECT
                a.id AS activityId,
                a.thumbnail_url AS activityThumbnailUri,
                a.title AS activityTitle,
                a.recruit_end_at AS activityRecruitEndAt,
                af.id AS activityFavoriteId,
                a.like_count AS activityFavoriteCount,
                COUNT(DISTINCT at.tag_id) AS recommendScore
            FROM activities a
            JOIN activity_tags at
                ON at.activity_id = a.id
            JOIN uot_result ur
                ON ur.tag_id = at.tag_id
            LEFT JOIN activity_favorites af
                ON af.activity_id = a.id
               AND af.user_id = :userId
            WHERE a.is_active = true
            GROUP BY
                a.id,
                a.thumbnail_url,
                a.title,
                a.recruit_end_at,
                af.id,
                a.like_count
            ORDER BY
                recommendScore DESC,
                a.like_count DESC
            """,
            nativeQuery = true
    )
    List<ActivityRecommendationProjection> findRecommendedActivitiesByUserId(
            @Param("userId") Long userId
    );
}
