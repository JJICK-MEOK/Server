package com.jjikmeok.app.domain.tag.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TagGroupType {
    MOOD("활동 분위기", "활동에서 느껴지는 전체적인 정서와 무드"),
    INTENSITY("활동 강도", "활동에 필요한 부담감, 몰입도, 도전 정도"),
    PRICE("활동 금액", "활동 참여 비용 여부"),
    PURPOSE("활동 목적", "사용자가 활동을 통해 얻고 싶은 것"),
    DURATION("활동 기간", "활동이 지속되는 기간");

    private final String label;
    private final String description;
}
