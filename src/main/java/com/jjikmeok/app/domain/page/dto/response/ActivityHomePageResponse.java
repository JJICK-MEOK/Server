package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityHomePageResponse(
        UserResponse user,
        List<ActivityCardResponse> recommendedActivities,
        List<ActivityCardResponse> closingSoonActivities
) {
    public record UserResponse(
            String nickname,
            String profileImageUrl
    ) {
    }
}

