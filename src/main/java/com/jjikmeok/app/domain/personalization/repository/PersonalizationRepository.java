package com.jjikmeok.app.domain.personalization.repository;

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
                    FROM user_onboardings uo
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
            WITH user_tags AS (
                SELECT DISTINCT
                    uot.tag_id,
                    t.name
                FROM users u
                JOIN user_onboardings uo
                    ON uo.user_id = u.id
                JOIN user_onboarding_tags uot
                    ON uot.user_onboarding_id = uo.id
                JOIN tags t
                    ON t.id = uot.tag_id
                WHERE u.id = :userId
            ),
            onboarding_tag_scores AS (
                SELECT
                    a.id AS activity_id,
                    COUNT(DISTINCT at.tag_id) AS onboarding_tag_match_count
                FROM activities a
                JOIN activity_tags at
                    ON at.activity_id = a.id
                JOIN user_tags ut
                    ON ut.tag_id = at.tag_id
                WHERE a.is_active = true
                GROUP BY a.id
            ),
            tag_name_scores AS (
                SELECT
                    a.id AS activity_id,
                    COUNT(DISTINCT t.name) AS tag_match_count
                FROM activities a
                JOIN activity_tags at
                    ON at.activity_id = a.id
                JOIN tags t
                    ON t.id = at.tag_id
                JOIN (
                    SELECT DISTINCT name
                    FROM user_tags
                ) user_tag_names
                    ON user_tag_names.name = t.name
                WHERE a.is_active = true
                GROUP BY a.id
            )
            SELECT
                a.id AS "activityId",
                a.title AS "title",
                a.thumbnail_url AS "thumbnailUrl",
                a.recruit_end_at AS "recruitEndAt",
                af.id AS "activityFavoriteId",
                tag.name AS "tagName"
            FROM onboarding_tag_scores ots
            JOIN activities a
                ON a.id = ots.activity_id
            JOIN activity_tags at
                ON at.activity_id = a.id
            JOIN tags tag
                ON tag.id = at.tag_id
            LEFT JOIN activity_favorites af
                ON af.activity_id = a.id
               AND af.user_id = :userId
            LEFT JOIN tag_name_scores tns
                ON tns.activity_id = a.id
            ORDER BY
                ots.onboarding_tag_match_count DESC,
                COALESCE(tns.tag_match_count, 0) DESC,
                a.like_count DESC,
                a.id ASC,
                tag.name ASC
            """,
            nativeQuery = true
    )
    List<ActivityRecommendationProjection> findRecommendedActivitiesByUserId(
            @Param("userId") Long userId
    );
}
