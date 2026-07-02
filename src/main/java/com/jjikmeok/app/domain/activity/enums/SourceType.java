package com.jjikmeok.app.domain.activity.enums;

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
}
