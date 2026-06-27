package com.jjikmeok.app.domain.activity.privateactivity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoveryGroupSize {
    SMALL("소규모"),
    LARGE("대규모");

    private final String label;
}
