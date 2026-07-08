package com.jjikmeok.app.domain.activity.enums;

import java.util.List;

public enum SourceType {
    KOPIS,
    EXHIBITION,
    SEOUL_CULTURE,
    SEOUL_RESERVATION,
    DISCOVERY,
    URL_MANUAL

    ;

    public boolean isPublicApiSource() {
        return this == KOPIS
                || this == EXHIBITION
                || this == SEOUL_CULTURE
                || this == SEOUL_RESERVATION;
    }

    public static List<SourceType> publicApiSources() {
        return List.of(KOPIS, EXHIBITION, SEOUL_CULTURE, SEOUL_RESERVATION);
    }
}
