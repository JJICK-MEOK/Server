package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ActivityType {
    PROGRAM("프로그램"),
    ONE_DAY("원데이"),
    EVENT("행사·강연"),
    CLUB("동아리");

    private final String label;

    public static boolean containsLabel(String label) {
        return Arrays.stream(values())
                .anyMatch(type -> type.label.equals(label));
    }
}
