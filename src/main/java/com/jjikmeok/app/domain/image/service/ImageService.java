package com.jjikmeok.app.domain.image.service;

import com.jjikmeok.app.domain.image.dto.request.ImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ImageResponse;

import java.util.List;

public interface ImageService {

    List<ImageResponse> getImages(Long activityId);

    ImageResponse createImage(Long activityId, ImageRequest request);

    ImageResponse updateImage(Long activityId, Long imageId, ImageRequest request);

    void deleteImage(Long activityId, Long imageId);
}
