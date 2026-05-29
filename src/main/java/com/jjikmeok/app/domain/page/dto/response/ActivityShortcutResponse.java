package com.jjikmeok.app.domain.page.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityType;

public record ActivityShortcutResponse(
        ActivityType type,
        String label,
        String icon,
        String href
) {
}
