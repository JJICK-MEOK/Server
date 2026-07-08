package com.jjikmeok.app.domain.activity.privateactivity.keyword;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;

public record DiscoveryBrandSource(
        String name,
        String domain,
        ActivityCategory category,
        boolean enabled,
        String note
) {
}
