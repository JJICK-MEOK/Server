package com.jjikmeok.app.domain.page.dto.response;

public record ActivityHomePopularActivityCardResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String activityType,
        Integer deadline,
        Boolean liked
) {
}
