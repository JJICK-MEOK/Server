package com.jjikmeok.app.domain.region.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegionDepth {
    PROVINCE(1, "시/도"),
    DISTRICT(2, "시/군/구");

    private final int value;
    private final String description;
}