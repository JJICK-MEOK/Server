package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ActivityCategory {
    SPORTS("운동 / 액티비티"),
    CULTURE("문화 / 예술"),
    CRAFT("공예 / 만들기"),
    COOKING("요리 / 베이킹"),
    PHOTO_VIDEO("사진 / 영상"),
    HUMANITIES("책 / 글"),
    TRAVEL("여행 / 탐방"),
    LANGUAGE("언어 / 해외"),
    VOLUNTEER("봉사활동"),
    CAREER("성장 / 커리어");

    private final String label;

    public static boolean containsLabel(String label) {
        return Arrays.stream(values())
                .anyMatch(category -> category.label.equals(label));
    }
}
