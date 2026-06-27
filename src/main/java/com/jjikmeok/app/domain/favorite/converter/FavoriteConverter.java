package com.jjikmeok.app.domain.favorite.converter;

import com.jjikmeok.app.domain.favorite.dto.response.FavoriteResponse;
import com.jjikmeok.app.domain.favorite.entity.Favorite;

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
