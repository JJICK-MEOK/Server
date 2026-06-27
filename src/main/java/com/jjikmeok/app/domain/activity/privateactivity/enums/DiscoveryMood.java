package com.jjikmeok.app.domain.activity.privateactivity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoveryMood {
    CALM("편안한"),
    HEALING("힐링"),
    LIVELY("활기찬"),
    EMOTIONAL("감성적"),
    CREATIVE("창의적"),
    TRENDY("트렌디");

    private final String label;
}
