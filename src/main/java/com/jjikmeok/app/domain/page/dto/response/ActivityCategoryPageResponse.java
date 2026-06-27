package com.jjikmeok.app.domain.page.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import java.util.List;

public record ActivityCategoryPageResponse(
        String pageTitle,
        ActivityType selectedType,
        ActivityCategory selectedCategory,
        String selectedSort,
        Long totalCount,
        List<ActivityFilterOptionResponse> typeTabs,
        List<ActivityFilterOptionResponse> categoryChips,
        List<ActivityFilterOptionResponse> sortOptions,
        List<ActivityCardResponse> activities
) {
}

