package com.jjikmeok.app.domain.personalization.dto;

import java.time.LocalDateTime;

public record ActivityRecommendationResponse(
        Long activityId,
        String activityThumbnailUri,
        String activityTitle,
        LocalDateTime activityRecruitEndAt,
        Long activityFavoriteId,
        int activityFavoriteCount,
        int recommendScore
) {

    public static ActivityRecommendationResponse from(ActivityRecommendationProjection projection) {
        return new ActivityRecommendationResponse(
                projection.getActivityId(),
                projection.getActivityThumbnailUri(),
                projection.getActivityTitle(),
                projection.getActivityRecruitEndAt(),
                projection.getActivityFavoriteId(),
                projection.getActivityFavoriteCount() == null ? 0 : projection.getActivityFavoriteCount(),
                toInt(projection.getRecommendScore())
        );
    }

    private static int toInt(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }
}
