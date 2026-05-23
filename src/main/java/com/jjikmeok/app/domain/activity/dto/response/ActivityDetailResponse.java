package com.jjikmeok.app.domain.activity.dto.response;

import com.jjikmeok.app.domain.activity.enums.AgeRange;
import java.time.LocalDateTime;

public record ActivityDetailResponse(
        Long id,
        Long regionId,
        String regionName,
        String title,
        String thumbnailUrl,
        String uri,
        String location,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        LocalDateTime activityStartAt,
        LocalDateTime activityEndAt,
        AgeRange ageRange,
        Integer price,
        String description,
        Integer viewCount,
        Integer likeCount,
        Integer reviewCount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}