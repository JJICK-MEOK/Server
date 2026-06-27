package com.jjikmeok.app.domain.page.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityCardResponse(
        Long id,
        String title,
        String thumbnailUrl,
        Integer deadline,
        Long regionId,
        String regionName,
        String address,
        ActivityType activityType,
        ActivityCategory category,
        List<String> hashtags,
        Integer price,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        Boolean liked,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt
) {
}

