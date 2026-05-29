package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record HomePageResponse(
        String nickname,
        Banner banner,
        List<ActivityShortcutResponse> shortcuts,
        RecommendedSection recommended,
        ClosingSoonSection closingSoon
) {
    public record Banner(
            String title,
            String subtitle,
            String actionLabel,
            String actionHref,
            Integer currentIndex,
            Integer totalCount
    ) {
    }

    public record RecommendedSection(
            String title,
            String actionHref,
            List<RecommendedActivity> activities
    ) {
    }

    public record ClosingSoonSection(
            String title,
            String actionHref,
            String theme,
            List<ClosingSoonActivity> activities
    ) {
    }

    public record RecommendedActivity(
            Long id,
            String title,
            String thumbnailUrl,
            String categoryLabel,
            String dDay,
            List<String> hashtags,
            Boolean liked
    ) {
    }

    public record ClosingSoonActivity(
            Long id,
            String title,
            String summary,
            String thumbnailUrl,
            String dDay,
            String badgeLabel,
            String categoryLabel,
            Boolean liked
    ) {
    }
}
