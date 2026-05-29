package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.response.ActivityFavoriteResponse;
import com.jjikmeok.app.domain.activity.entity.ActivityFavorite;

public class ActivityFavoriteConverter {

    private ActivityFavoriteConverter() {
    }

    public static ActivityFavoriteResponse toResponse(ActivityFavorite favorite) {
        return new ActivityFavoriteResponse(
                favorite.getId(),
                favorite.getUser().getId(),
                favorite.getActivity().getId(),
                favorite.getCreatedAt()
        );
    }
}
