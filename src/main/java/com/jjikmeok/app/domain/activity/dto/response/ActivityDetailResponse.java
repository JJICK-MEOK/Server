package com.jjikmeok.app.domain.activity.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityDetailResponse(
        Long id,
        Long regionId,
        String regionName,
        String title,
        String description,
        String thumbnailUrl,
        String sourceUrl,
        String address,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        Integer price,
        ActivityType activityType,
        ActivityCategory category,
        List<String> tags,
        SourceType sourceType,
        String externalId,
        ApprovalStatus approvalStatus,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
