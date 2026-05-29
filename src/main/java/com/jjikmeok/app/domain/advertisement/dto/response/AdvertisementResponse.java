package com.jjikmeok.app.domain.advertisement.dto.response;

import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;

import java.time.LocalDateTime;

public record AdvertisementResponse(
        Long id,
        String title,
        String imageUrl,
        String redirectUrl,
        AdvertisementPosition position,
        Integer sortOrder,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer viewCount,
        Integer clickCount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
