package com.jjikmeok.app.domain.discovery.enums;

public enum DiscoverySheetStatus {
    PENDING,
    REVIEWING,
    READY,
    PUBLISHED,
    DUPLICATE,
    REJECTED,
    ERROR;

    public boolean isTerminal() {
        return this == PUBLISHED || this == DUPLICATE || this == REJECTED || this == ERROR;
    }
}
