package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityHomePageResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;

public interface ActivityPageService {

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
