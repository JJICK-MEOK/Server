package com.jjikmeok.app.domain.activity.dto.response.page;

public record ActivityImageItemResponse(
        Long id,
        String imageUrl,
        Integer sortOrder,
        Boolean thumbnail
) {
}
