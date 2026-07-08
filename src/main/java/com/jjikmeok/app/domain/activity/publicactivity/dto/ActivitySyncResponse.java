package com.jjikmeok.app.domain.activity.publicactivity.dto;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Public activity sync result")
public class ActivitySyncResponse {

    @Schema(description = "Synced source type", example = "KOPIS")
    private final SourceType sourceType;

    @Schema(description = "Number of raw payloads archived", example = "3")
    private final int rawSavedCount;

    @Schema(description = "Number of activities staged into Google Sheets", example = "24")
    private final int activitySavedCount;

    @Schema(description = "Number of duplicate activities updated or skipped", example = "7")
    private final int duplicatedCount;
}
