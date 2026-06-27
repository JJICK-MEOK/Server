package com.jjikmeok.app.domain.page.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityCardResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String dDay,
        Long daysUntilRecruitEnd,
        String deadlineText,
        Long regionId,
        String regionName,
        String address,
        ActivityType activityType,
        String activityTypeLabel,
        ActivityCategory category,
        String categoryLabel,
        List<String> hashtags,
        Integer price,
        String priceLabel,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        Boolean liked,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitEndAt
) {
}

