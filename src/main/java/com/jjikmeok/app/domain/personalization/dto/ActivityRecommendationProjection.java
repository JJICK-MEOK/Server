package com.jjikmeok.app.domain.personalization.dto;

import java.time.LocalDateTime;

public interface ActivityRecommendationProjection {

    Long getActivityId();

    String getActivityThumbnailUri();

    String getActivityTitle();

    LocalDateTime getActivityRecruitEndAt();

    Long getActivityFavoriteId();

    Integer getActivityFavoriteCount();

    Integer getRecommendScore();
}
