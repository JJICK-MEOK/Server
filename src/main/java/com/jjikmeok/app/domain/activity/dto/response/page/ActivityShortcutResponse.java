package com.jjikmeok.app.domain.activity.dto.response.page;

import com.jjikmeok.app.domain.activity.enums.ActivityType;

public record ActivityShortcutResponse(
        ActivityType type,
        String label,
        String icon,
        String href
) {
}
