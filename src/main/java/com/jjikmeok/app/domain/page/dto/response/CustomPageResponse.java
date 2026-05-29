package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record CustomPageResponse(
        String nickname,
        TasteProfile tasteProfile,
        ActivitySectionResponse recommended
) {
    public record TasteProfile(
            String title,
            String subtitle,
            List<String> hashtags
    ) {
    }
}
