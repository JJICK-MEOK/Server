package com.jjikmeok.app.domain.image.converter;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.image.dto.request.ImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ImageResponse;
import com.jjikmeok.app.domain.image.entity.Image;

public class ImageConverter {

    private ImageConverter() {
    }

    public static Image toEntity(Activity activity, ImageRequest request) {
        return Image.create(activity, request.imageUrl().trim(), request.sortOrder(), request.isThumbnail());
    }

    public static ImageResponse toResponse(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getActivity().getId(),
                image.getImageUrl(),
                image.getSortOrder(),
                image.getIsThumbnail(),
                image.getCreatedAt()
        );
    }
}
