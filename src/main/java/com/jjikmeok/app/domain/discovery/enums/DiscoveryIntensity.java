package com.jjikmeok.app.domain.discovery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoveryIntensity {
    BEGINNER("입문"),
    LIGHT("가볍게"),
    IMMERSIVE("몰입"),
    CHALLENGE("도전");

    private final String label;
}
