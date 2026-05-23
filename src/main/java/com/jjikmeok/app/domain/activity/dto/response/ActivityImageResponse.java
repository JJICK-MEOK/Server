package com.jjikmeok.app.domain.activity.dto.response;

import java.time.LocalDateTime;

public record ActivityImageResponse(
        Long id,
        Long activityId,
        String imageUrl,
        Integer sortOrder,
        Boolean isThumbnail,
        LocalDateTime createdAt
) {
}
