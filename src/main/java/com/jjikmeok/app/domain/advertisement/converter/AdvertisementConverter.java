package com.jjikmeok.app.domain.advertisement.converter;

import com.jjikmeok.app.domain.advertisement.dto.request.AdvertisementRequest;
import com.jjikmeok.app.domain.advertisement.dto.response.AdvertisementResponse;
import com.jjikmeok.app.domain.advertisement.entity.Advertisement;

public class AdvertisementConverter {

    private AdvertisementConverter() {
    }

    public static Advertisement toEntity(AdvertisementRequest request) {
        return Advertisement.builder()
                .title(request.title().trim())
                .imageUrl(request.imageUrl().trim())
                .redirectUrl(request.redirectUrl().trim())
                .position(request.position())
                .sortOrder(request.sortOrder())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .isActive(request.isActive())
                .build();
    }

    public static AdvertisementResponse toResponse(Advertisement advertisement) {
        return new AdvertisementResponse(
                advertisement.getId(),
                advertisement.getTitle(),
                advertisement.getImageUrl(),
                advertisement.getRedirectUrl(),
                advertisement.getPosition(),
                advertisement.getSortOrder(),
                advertisement.getStartAt(),
                advertisement.getEndAt(),
                advertisement.getViewCount(),
                advertisement.getClickCount(),
                advertisement.getIsActive(),
                advertisement.getCreatedAt(),
                advertisement.getUpdatedAt()
        );
    }
}
