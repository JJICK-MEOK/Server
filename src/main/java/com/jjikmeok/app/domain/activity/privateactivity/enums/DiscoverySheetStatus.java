package com.jjikmeok.app.domain.activity.privateactivity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscoverySheetStatus {
    PENDING("검토중"),
    REVIEWING("검토완료"),
    READY("발행대기"),
    PUBLISHED("발행완료"),
    DUPLICATE("중복"),
    REJECTED("반려"),
    ERROR("오류");

    private final String label;

    public boolean isTerminal() {
        return this == PUBLISHED || this == DUPLICATE || this == REJECTED || this == ERROR;
    }
}
