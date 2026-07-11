package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityHomeCurationCardResponse(
        String title,
        String thumbnailUrl,
        List<String> hashtags
) {
}
