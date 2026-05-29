package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    CALM("차분", PreferenceTagGroup.MOOD),
    LIVELY("활기", PreferenceTagGroup.MOOD),
    HEALING("힐링", PreferenceTagGroup.MOOD),
    COMFORTABLE("편안", PreferenceTagGroup.MOOD),

    BEGINNER("입문", PreferenceTagGroup.INTENSITY),
    LIGHT("가볍게", PreferenceTagGroup.INTENSITY),
    IMMERSIVE("몰입", PreferenceTagGroup.INTENSITY),
    CHALLENGE("도전", PreferenceTagGroup.INTENSITY),

    FREE("무료", PreferenceTagGroup.PRICE),
    PAID("유료", PreferenceTagGroup.PRICE),

    REST("휴식", PreferenceTagGroup.PURPOSE),
    HOBBY("취미", PreferenceTagGroup.PURPOSE),
    LEARNING("배움", PreferenceTagGroup.PURPOSE),
    SOCIAL("사교", PreferenceTagGroup.PURPOSE),
    GROWTH("성장", PreferenceTagGroup.PURPOSE),
    EXPERIENCE("경험", PreferenceTagGroup.PURPOSE),

    ONE_DAY("하루", PreferenceTagGroup.DURATION),
    THREE_DAYS("3일", PreferenceTagGroup.DURATION),
    ONE_WEEK("일주일", PreferenceTagGroup.DURATION),
    ONE_MONTH("한달", PreferenceTagGroup.DURATION),
    THREE_MONTHS("3개월", PreferenceTagGroup.DURATION),
    OVER_SIX_MONTHS("6개월이상", PreferenceTagGroup.DURATION),
    OVER_ONE_YEAR("1년이상", PreferenceTagGroup.DURATION);

    private final String label;
    private final PreferenceTagGroup group;

    public String getHashtag() {
        return "#" + label;
    }

    public static boolean containsLabel(String label) {
        return Arrays.stream(values())
                .anyMatch(tag -> tag.label.equals(label));
    }
}
