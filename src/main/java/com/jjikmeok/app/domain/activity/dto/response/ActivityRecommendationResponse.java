package com.jjikmeok.app.domain.activity.dto.response;

import com.jjikmeok.app.domain.activity.entity.Activity;

public record ActivityRecommendationResponse(
        Activity activity,
        Boolean liked
) {
}
