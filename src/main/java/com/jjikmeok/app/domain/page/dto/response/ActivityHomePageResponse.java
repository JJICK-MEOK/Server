package com.jjikmeok.app.domain.page.dto.response;

public record ActivityHomePageResponse(
        UserResponse user,
        ActivityHomeCurationSectionResponse featured,
        ActivityHomeActivitySectionResponse popular,
        ActivityHomeActivitySectionResponse expandedRecommendation
) {
    public record UserResponse(
            String nickname,
            String profileImageUrl
    ) {
    }
}

