package com.jjikmeok.app.domain.image.dto.response;

import java.time.LocalDateTime;

public record ImageResponse(
        Long id,
        Long activityId,
        String imageUrl,
        Integer sortOrder,
        Boolean isThumbnail,
        LocalDateTime createdAt
) {
}
