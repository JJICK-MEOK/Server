package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreferenceTagGroup {
    MOOD("분위기 태그", "활동의 전체적인 분위기를 나타내는 태그"),
    INTENSITY("활동 강도", "활동에 필요한 부담감, 몰입도, 도전 정도"),
    PURPOSE("활동 목적", "사용자가 활동을 통해 얻고 싶은 것"),
    DURATION("활동 기간", "활동이 지속되는 기간"),
    SIZE("활동 규모", "활동의 참여 규모");

    private final String label;
    private final String description;
}
