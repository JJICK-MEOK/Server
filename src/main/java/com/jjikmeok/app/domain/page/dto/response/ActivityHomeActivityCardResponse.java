package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityHomeActivityCardResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String activityType,
        Integer deadline,
        List<String> hashtags,
        Boolean liked
) {
}
