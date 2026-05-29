package com.jjikmeok.app.domain.activity.dto.response.page;

import java.util.List;

public record ActivitySectionResponse(
        String key,
        String title,
        String subtitle,
        List<ActivityCardResponse> activities
) {
}
