package com.jjikmeok.app.domain.activity.enums;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    CALM("차분", TagGroupType.MOOD),
    LIVELY("활기", TagGroupType.MOOD),
    HEALING("힐링", TagGroupType.MOOD),
    COMFORTABLE("편안", TagGroupType.MOOD),

    BEGINNER("입문", TagGroupType.INTENSITY),
    LIGHT("가볍게", TagGroupType.INTENSITY),
    IMMERSIVE("몰입", TagGroupType.INTENSITY),
    CHALLENGE("도전", TagGroupType.INTENSITY),

    FREE("무료", TagGroupType.PRICE),
    PAID("유료", TagGroupType.PRICE),

    REST("휴식", TagGroupType.PURPOSE),
    HOBBY("취미", TagGroupType.PURPOSE),
    LEARNING("배움", TagGroupType.PURPOSE),
    SOCIAL("사교", TagGroupType.PURPOSE),
    GROWTH("성장", TagGroupType.PURPOSE),
    EXPERIENCE("경험", TagGroupType.PURPOSE),

    ONE_DAY("하루", TagGroupType.DURATION),
    THREE_DAYS("3일", TagGroupType.DURATION),
    ONE_WEEK("일주일", TagGroupType.DURATION),
    ONE_MONTH("한달", TagGroupType.DURATION),
    THREE_MONTHS("3개월", TagGroupType.DURATION),
    OVER_SIX_MONTHS("6개월이상", TagGroupType.DURATION),
    OVER_ONE_YEAR("1년이상", TagGroupType.DURATION);

    private final String label;
    private final TagGroupType group;

    public String getHashtag() {
        return "#" + label;
    }

    public static boolean containsLabel(String label) {
        return Arrays.stream(values())
                .anyMatch(tag -> tag.label.equals(label));
    }
}
