package com.jjikmeok.app.domain.page.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityDetailPageResponse(
        Long id,
        Long regionId,
        String regionName,
        String title,
        String description,
        String thumbnailUrl,
        List<ImageItemResponse> images,
        String sourceUrl,
        String address,
        String organizer,
        String contactInfo,
        String target,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        Integer deadline,
        Integer price,
        ActivityType activityType,
        ActivityCategory category,
        List<String> hashtags,
        SourceType sourceType,
        String externalId,
        ApprovalStatus approvalStatus,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        Boolean liked,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

