package com.jjikmeok.app.domain.activity.dto.response;

import com.jjikmeok.app.domain.activity.enums.AgeRange;

import java.time.LocalDateTime;

public record ActivitySummaryResponse(
        Long id,
        Long regionId,
        String regionName,
        String title,
        String thumbnailUrl,
        String location,
        LocalDateTime recruitEndAt,
        LocalDateTime activityStartAt,
        LocalDateTime activityEndAt,
        AgeRange ageRange,
        Integer price,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        LocalDateTime createdAt
) {
}
