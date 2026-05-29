package com.jjikmeok.app.domain.image.service;

import com.jjikmeok.app.domain.image.dto.request.ActivityImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ActivityImageResponse;

import java.util.List;

public interface ActivityImageService {

    List<ActivityImageResponse> getActivityImages(Long activityId);

    ActivityImageResponse createActivityImage(Long activityId, ActivityImageRequest request);

    ActivityImageResponse updateActivityImage(Long activityId, Long imageId, ActivityImageRequest request);

    void deleteActivityImage(Long activityId, Long imageId);
}
