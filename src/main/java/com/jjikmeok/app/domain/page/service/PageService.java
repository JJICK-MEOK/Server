package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;

public interface PageService {

    ActivityHomePageResponse getHomePage(Long userId, Integer limit);

    ActivityCategoryPageResponse getCategoryPage(
            Long userId,
            ActivityType type,
            ActivityCategory category,
            String sort,
            Integer limit
    );

    ActivityCustomPageResponse getCustomPage(Long userId, Integer limit);

    ActivityDetailPageResponse getDetailPage(Long userId, Long activityId);
}

