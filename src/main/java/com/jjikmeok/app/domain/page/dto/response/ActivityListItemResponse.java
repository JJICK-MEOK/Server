package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityListItemResponse(
        Long id,
        String title,
        String thumbnailUrl,
        String dDay,
        Integer viewCount,
        Integer likeCount,
        List<String> hashtags,
        Boolean liked
) {
}
