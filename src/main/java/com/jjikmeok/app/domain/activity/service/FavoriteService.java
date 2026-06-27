package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.FavoriteRequest;
import com.jjikmeok.app.domain.activity.dto.response.FavoriteResponse;

import java.util.List;

public interface FavoriteService {
    List<FavoriteResponse> getFavorites(Long userId);
    FavoriteResponse createFavorite(Long userId, FavoriteRequest request);
    void deleteFavorite(Long userId, Long activityId);
}
