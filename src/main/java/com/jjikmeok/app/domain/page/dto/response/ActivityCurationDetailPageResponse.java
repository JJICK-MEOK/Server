package com.jjikmeok.app.domain.page.dto.response;

import java.util.List;

public record ActivityCurationDetailPageResponse(
        String title,
        String subtitle,
        List<String> hashtags,
        List<ActivityHomeActivityCardResponse> activities,
        Integer page,
        Integer limit,
        Boolean hasNext,
        Integer nextPage
) {
}
