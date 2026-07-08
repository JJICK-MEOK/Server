package com.jjikmeok.app.domain.activity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PublicActivitySourceType",
        description = "Public activity source type",
        allowableValues = {"KOPIS", "EXHIBITION", "SEOUL_CULTURE", "SEOUL_RESERVATION"}
)
public enum PublicActivitySourceType {
    KOPIS(SourceType.KOPIS),
    EXHIBITION(SourceType.EXHIBITION),
    SEOUL_CULTURE(SourceType.SEOUL_CULTURE),
    SEOUL_RESERVATION(SourceType.SEOUL_RESERVATION);

    private final SourceType sourceType;

    PublicActivitySourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public SourceType toSourceType() {
        return sourceType;
    }
}
