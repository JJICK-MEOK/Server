package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityFavoriteRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityFavoriteResponse;

import java.util.List;

public interface ActivityFavoriteService {
    List<ActivityFavoriteResponse> getFavorites(Long userId);
    ActivityFavoriteResponse createFavorite(Long userId, ActivityFavoriteRequest request);
    void deleteFavorite(Long userId, Long activityId);
}
