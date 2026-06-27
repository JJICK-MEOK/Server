package com.jjikmeok.app.domain.page.dto.response;

public record ActivityImageItemResponse(
        Long id,
        String imageUrl,
        Integer sortOrder,
        Boolean thumbnail
) {
}

