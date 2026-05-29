package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.page.dto.response.CategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.CustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.HomePageResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;

public interface PageService {

    HomePageResponse getHomePage(Long userId, Integer limit);

    CategoryPageResponse getCategoryPage(
            Long userId,
            ActivityType type,
            ActivityCategory category,
            String sort,
            Integer limit
    );

    CustomPageResponse getCustomPage(Long userId, Integer limit);

    ActivityDetailPageResponse getDetailPage(Long userId, Long activityId);
}
