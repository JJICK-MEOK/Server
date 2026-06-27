package com.jjikmeok.app.domain.discovery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoveryPurpose {
    REST("휴식"),
    HOBBY("취미"),
    LEARNING("배움"),
    GROWTH("성장");

    private final String label;
}
