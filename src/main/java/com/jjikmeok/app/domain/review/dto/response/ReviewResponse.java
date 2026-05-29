package com.jjikmeok.app.domain.review.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
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
