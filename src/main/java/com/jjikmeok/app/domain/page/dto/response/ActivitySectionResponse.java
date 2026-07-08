package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivitySectionResponse(
        List<ActivityCardResponse> activities
) {
}

