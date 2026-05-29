package com.jjikmeok.app.domain.image.converter;

import com.jjikmeok.app.domain.image.dto.request.ActivityImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ActivityImageResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.image.entity.ActivityImage;

public class ActivityImageConverter {

    private ActivityImageConverter() {
    }

    public static ActivityImage toEntity(Activity activity, ActivityImageRequest request) {
        return ActivityImage.create(activity, request.imageUrl().trim(), request.sortOrder(), request.isThumbnail());
    }

    public static ActivityImageResponse toResponse(ActivityImage activityImage) {
        return new ActivityImageResponse(
                activityImage.getId(),
                activityImage.getActivity().getId(),
                activityImage.getImageUrl(),
                activityImage.getSortOrder(),
                activityImage.getIsThumbnail(),
                activityImage.getCreatedAt()
        );
    }
}
