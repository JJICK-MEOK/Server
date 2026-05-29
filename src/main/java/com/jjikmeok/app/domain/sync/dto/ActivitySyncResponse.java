package com.jjikmeok.app.domain.sync.dto;

import com.jjikmeok.app.domain.activity.enums.SourceType;

public record ActivitySyncResponse(
        SourceType sourceType,
        int rawSavedCount,
        int activitySavedCount,
        int duplicatedCount
) {
}
