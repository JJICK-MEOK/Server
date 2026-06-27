package com.jjikmeok.app.domain.page.dto.response;

public record ImageItemResponse(
        Long id,
        String imageUrl,
        Integer sortOrder,
        Boolean thumbnail
) {
}
