package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityHomePopularActivitySectionResponse(
        List<ActivityHomePopularActivityCardResponse> activities
) {
}
