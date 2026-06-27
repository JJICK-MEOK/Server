package com.jjikmeok.app.domain.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    CALM("편안한", PreferenceTagGroup.MOOD),
    HEALING("힐링", PreferenceTagGroup.MOOD),
    LIVELY("활기찬", PreferenceTagGroup.MOOD),
    EMOTIONAL("감성적", PreferenceTagGroup.MOOD),
    CREATIVE("창의적", PreferenceTagGroup.MOOD),
    TRENDY("트렌디", PreferenceTagGroup.MOOD),

    BEGINNER("입문", PreferenceTagGroup.INTENSITY),
    LIGHT("가볍게", PreferenceTagGroup.INTENSITY),
    IMMERSIVE("몰입", PreferenceTagGroup.INTENSITY),
    CHALLENGE("도전", PreferenceTagGroup.INTENSITY),

    REST("휴식", PreferenceTagGroup.PURPOSE),
    HOBBY("취미", PreferenceTagGroup.PURPOSE),
    LEARNING("배움", PreferenceTagGroup.PURPOSE),
    GROWTH("성장", PreferenceTagGroup.PURPOSE),

    SHORT_TERM("단기", PreferenceTagGroup.DURATION),
    ONE_MONTH("한달", PreferenceTagGroup.DURATION),
    SIX_MONTHS("6개월", PreferenceTagGroup.DURATION),
    OVER_ONE_YEAR("1년 이상", PreferenceTagGroup.DURATION),

    ONE_DAY("단기", PreferenceTagGroup.DURATION),
    THREE_DAYS("단기", PreferenceTagGroup.DURATION),
    ONE_WEEK("단기", PreferenceTagGroup.DURATION),
    THREE_MONTHS("한달", PreferenceTagGroup.DURATION),
    OVER_SIX_MONTHS("6개월", PreferenceTagGroup.DURATION),

    SMALL("소규모", PreferenceTagGroup.SIZE),
    LARGE("대규모", PreferenceTagGroup.SIZE),

    SOCIAL("사교", PreferenceTagGroup.PURPOSE),
    EXPERIENCE("경험", PreferenceTagGroup.PURPOSE);

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
