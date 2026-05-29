package com.jjikmeok.app.domain.activity.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record ActivitySummaryResponse(
        Long id,
        Long regionId,
        String regionName,
        String title,
        String thumbnailUrl,
        String address,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitEndAt,
        ActivityType activityType,
        ActivityCategory category,
        List<String> tags,
        Integer price,
        SourceType sourceType,
        ApprovalStatus approvalStatus,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        LocalDateTime createdAt
) {
}
