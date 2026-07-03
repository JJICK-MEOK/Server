package com.jjikmeok.app.domain.personalization.dto;

import java.time.LocalDateTime;

public interface ActivityRecommendationProjection {

    Long getActivityId();

    String getTitle();

    String getThumbnailUrl();

    LocalDateTime getRecruitEndAt();

    Long getActivityFavoriteId();

    String getTagName();
}
