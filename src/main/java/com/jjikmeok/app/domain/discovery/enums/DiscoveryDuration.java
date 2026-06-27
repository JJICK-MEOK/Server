package com.jjikmeok.app.domain.discovery.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoveryDuration {
    SHORT_TERM("단기"),
    ONE_MONTH("한달"),
    SIX_MONTHS("6개월"),
    OVER_ONE_YEAR("1년 이상");

    private final String label;
}
