package com.jjikmeok.app.domain.activity.dto.response;

import java.time.LocalDateTime;

public record ActivityFavoriteResponse(
        Long id,
        Long userId,
        Long activityId,
        LocalDateTime createdAt
) {
}
