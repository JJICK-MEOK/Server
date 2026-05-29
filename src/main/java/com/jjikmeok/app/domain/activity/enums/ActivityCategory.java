package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ActivityCategory {
    SPORTS("운동/액티비티"),
    CULTURE("문화/공연/축제"),
    CRAFT("공예/만들기"),
    DANCE("댄스/무용"),
    COOKING("요리/베이킹"),
    PHOTO_VIDEO("사진/영상"),
    MUSIC("음악/악기"),
    HUMANITIES("인문학/책/글"),
    TRAVEL("여행/산책/탐방"),
    LANGUAGE("해외/언어"),
    VOLUNTEER("봉사활동"),
    SELF_DEVELOPMENT("자기계발/클래스"),
    CAREER("커리어/실무"),
    ETC("기타");

    private final String label;

    public static boolean containsLabel(String label) {
        return Arrays.stream(values())
                .anyMatch(category -> category.label.equals(label));
    }
}
