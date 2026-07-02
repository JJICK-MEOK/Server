package com.jjikmeok.app.domain.activity.enums;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PreferenceTag {
    CALM("편안한", TagGroupType.MOOD),
    HEALING("힐링", TagGroupType.MOOD),
    LIVELY("활기찬", TagGroupType.MOOD),
    EMOTIONAL("감성적", TagGroupType.MOOD),
    CREATIVE("창의적", TagGroupType.MOOD),
    TRENDY("트렌디", TagGroupType.MOOD),

    BEGINNER("입문", TagGroupType.INTENSITY),
    LIGHT("가볍게", TagGroupType.INTENSITY),
    IMMERSIVE("몰입", TagGroupType.INTENSITY),
    CHALLENGE("도전", TagGroupType.INTENSITY),

    REST("휴식", TagGroupType.PURPOSE),
    HOBBY("취미", TagGroupType.PURPOSE),
    LEARNING("배움", TagGroupType.PURPOSE),
    GROWTH("성장", TagGroupType.PURPOSE),

    SHORT_TERM("단기", TagGroupType.DURATION),
    ONE_MONTH("한달", TagGroupType.DURATION),
    SIX_MONTHS("6개월", TagGroupType.DURATION),
    OVER_ONE_YEAR("1년 이상", TagGroupType.DURATION),

    ONE_DAY("단기", TagGroupType.DURATION),
    THREE_DAYS("단기", TagGroupType.DURATION),
    ONE_WEEK("단기", TagGroupType.DURATION),
    THREE_MONTHS("한달", TagGroupType.DURATION),
    OVER_SIX_MONTHS("6개월", TagGroupType.DURATION),

    SMALL("소규모", TagGroupType.SIZE),
    LARGE("대규모", TagGroupType.SIZE),

    SOCIAL("사교", TagGroupType.PURPOSE),
    EXPERIENCE("경험", TagGroupType.PURPOSE);


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
