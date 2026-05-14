package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;

import java.util.List;

public interface ActivityService {
    List<ActivitySummaryResponse> getActivities(Long regionId);
    ActivityDetailResponse getActivity(Long activityId);
    ActivityDetailResponse createActivity(ActivityRequest request);
    ActivityDetailResponse updateActivity(Long id, ActivityRequest request);

    void deleteActivity(Long id);
}
