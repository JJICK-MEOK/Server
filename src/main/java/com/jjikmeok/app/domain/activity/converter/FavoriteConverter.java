package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.response.FavoriteResponse;
import com.jjikmeok.app.domain.activity.entity.Favorite;

public class FavoriteConverter {

    private FavoriteConverter() {
    }

    public static FavoriteResponse toResponse(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getUser().getId(),
                favorite.getActivity().getId(),
                favorite.getCreatedAt()
        );
    }
}
