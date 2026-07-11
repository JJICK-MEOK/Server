package com.jjikmeok.app.domain.page.model;

import java.util.Arrays;
import java.util.List;

public enum HomeCurationType {
    SOLO_CULTURE(
            "혼자 즐기는 문화생활",
            "혼자여도 부담 없이 즐길 수 있는 활동을 모았어요",
            List.of("#감성적", "#소규모"),
            "https://example.com/home-curations/solo-culture.png",
            List.of("감성적", "소규모", "편안한", "휴식", "가볍게")
    ),
    NEW_INSPIRATION(
            "새로운 영감이 필요할 때",
            "트렌디한 감각을 채워줄 활동들만 모았어요",
            List.of("#트렌디", "#창의적"),
            "https://example.com/home-curations/new-inspiration.png",
            List.of("트렌디", "창의적", "도전", "취미", "배움")
    ),
    WEEKEND_FUN(
            "이번 주말 뭐하지?",
            "가볍게 시작하기 좋은 활동만 모았어요",
            List.of("#가볍게", "#단기"),
            "https://example.com/home-curations/weekend-fun.png",
            List.of("가볍게", "단기", "취미", "입문", "활기찬")
    ),
    HEALING(
            "쉬어가고 싶은 날엔",
            "몸과 마음을 채워줄 활동을 추천해요",
            List.of("#힐링", "#휴식"),
            "https://example.com/home-curations/healing.png",
            List.of("힐링", "휴식", "편안한", "소규모", "차분한")
    ),
    ZERO_COST(
            "0원으로 취미 입문",
            "부담 없이 시작할 수 있는 무료 활동만 모았어요",
            List.of("#입문", "#취미"),
            "https://example.com/home-curations/zero-cost.png",
            List.of("입문", "취미", "가볍게", "단기", "소규모")
    ),
    FOCUS_TIME(
            "조용히 몰입하는 시간",
            "오롯이 나에게 집중하고 싶을 때 즐기기 좋은 활동들이에요",
            List.of("#감성적", "#몰입"),
            "https://example.com/home-curations/focus-time.png",
            List.of("감성적", "몰입", "편안한", "소규모", "취미")
    ),
    GROW_AS_YOU_LEARN(
            "배우면서 성장하기",
            "실력도 얻고 싶은 마음을 채울 수 있는 활동을 모았어요",
            List.of("#몰입", "#배움"),
            "https://example.com/home-curations/grow-as-you-learn.png",
            List.of("몰입", "배움", "성장", "도전", "가볍게")
    ),
    LONG_TERM_HOBBY(
            "오래 좋아할 무언가를 찾는다면",
            "꾸준히 이어갈 취미를 찾는 분께 추천해요",
            List.of("#6개월", "#1년이상"),
            "https://example.com/home-curations/long-term-hobby.png",
            List.of("6개월", "1년이상", "성장", "몰입", "취미")
    ),
    ACTIVE_CHANGE(
            "움직이면 기분이 달라질지도",
            "몸을 쓰며 기분 전환하기 좋은 활동만 모았어요",
            List.of("#활기찬", "#도전"),
            "https://example.com/home-curations/active-change.png",
            List.of("활기찬", "도전", "대규모", "단기", "가볍게")
    ),
    MEANINGFUL_DAY(
            "의미 있는 하루를 보내고 싶다면",
            "참여하는 동안 보람을 느낄 활동들을 모아봤어요",
            List.of("#성장", "#대규모"),
            "https://example.com/home-curations/meaningful-day.png",
            List.of("성장", "대규모", "배움", "도전", "1년이상")
    );

    private final String title;
    private final String subtitle;
    private final List<String> displayHashtags;
    private final String thumbnailUrl;
    private final List<String> matchTagNames;

    HomeCurationType(
            String title,
            String subtitle,
            List<String> displayHashtags,
            String thumbnailUrl,
            List<String> matchTagNames
    ) {
        this.title = title;
        this.subtitle = subtitle;
        this.displayHashtags = displayHashtags;
        this.thumbnailUrl = thumbnailUrl;
        this.matchTagNames = matchTagNames;
    }

    public String getKey() {
        return name();
    }

    public static HomeCurationType fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElse(null);
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public List<String> getDisplayHashtags() {
        return displayHashtags;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public List<String> getMatchTagNames() {
        return matchTagNames;
    }
}
