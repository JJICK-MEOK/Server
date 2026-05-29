package com.jjikmeok.app.domain.activity.dto.response.page;

import java.util.List;

public record ActivityHomePageResponse(
        String nickname,
        Hero hero,
        List<ActivityShortcutResponse> shortcuts,
        ActivitySectionResponse recommended,
        ActivitySectionResponse closingSoon
) {
    public record Hero(
            String title,
            String subtitle,
            String actionLabel,
            String actionHref
    ) {
    }
}
