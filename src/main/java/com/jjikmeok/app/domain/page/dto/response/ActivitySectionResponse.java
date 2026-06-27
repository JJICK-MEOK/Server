package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivitySectionResponse(
        String key,
        String title,
        String subtitle,
        List<ActivityCardResponse> activities
) {
}

