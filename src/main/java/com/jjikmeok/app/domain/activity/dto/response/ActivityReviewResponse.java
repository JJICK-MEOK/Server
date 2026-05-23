package com.jjikmeok.app.domain.activity.dto.response;

import java.time.LocalDateTime;

public record ActivityReviewResponse(
        Long id,
        Long userId,
        Long activityId,
        Integer rating,
        String reason,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
