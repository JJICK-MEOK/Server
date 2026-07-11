package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityHomeActivitySectionResponse(
        List<ActivityHomeActivityCardResponse> activities
) {
}
