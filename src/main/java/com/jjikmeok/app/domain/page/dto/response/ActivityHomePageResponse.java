package com.jjikmeok.app.domain.page.dto.response;

public record ActivityHomePageResponse(
        UserResponse user,
        ActivitySectionResponse featured,
        ActivitySectionResponse popular,
        ActivitySectionResponse expandedRecommendation
) {
    public record UserResponse(
            String nickname,
            String profileImageUrl
    ) {
    }
}

