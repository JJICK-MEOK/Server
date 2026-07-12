package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.personalization.dto.ActivityPersonalizationScoreProjection;
import com.jjikmeok.app.domain.personalization.entity.ActivityPreferenceVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityPreferenceVectorRepository extends JpaRepository<ActivityPreferenceVector, Long> {

    Optional<ActivityPreferenceVector> findByActivityId(Long activityId);

    @Query(
            value = """
                    SELECT
                        scored.activity_id AS "activityId",
                        CASE
                            WHEN CAST(scored.cosine_distance AS TEXT) = 'NaN' THEN NULL
                            ELSE CAST(
                                ROUND(
                                    GREATEST(
                                        0.0,
                                        LEAST(
                                            1.0,
                                            1 - scored.cosine_distance
                                        )
                                    ) * 100
                                ) AS INTEGER
                            )
                        END AS "personalizationScore"
                    FROM (
                        SELECT
                            apv.activity_id,
                            apv.embedding <=> upv.embedding AS cosine_distance
                        FROM activity_preference_vectors apv
                        JOIN user_preference_vectors upv
                            ON upv.user_id = :userId
                        WHERE apv.activity_id IN (:activityIds)
                    ) scored
                    """,
            nativeQuery = true
    )
    List<ActivityPersonalizationScoreProjection> findPersonalizationScores(
            @Param("userId") Long userId,
            @Param("activityIds") List<Long> activityIds
    );
}
