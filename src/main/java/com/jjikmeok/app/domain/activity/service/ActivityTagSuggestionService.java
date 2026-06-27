package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.PreferenceTagGroup;
import com.jjikmeok.app.domain.sync.service.ActivitySyncUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityTagSuggestionService {

    private final ActivitySyncUtils utils;

    public List<PreferenceTag> suggest(Activity activity) {
        if (activity == null) {
            return List.of();
        }

        return suggest(
                join(
                        activity.getTitle(),
                        activity.getDescription(),
                        activity.getOrganizer(),
                        activity.getContactInfo(),
                        activity.getTarget(),
                        activity.getAddress(),
                        activity.getSourceUrl()
                ),
                activity.getCategory(),
                activity.getPrice(),
                activity.getStartAt(),
                activity.getEndAt()
        );
    }

    public List<PreferenceTag> suggest(
            String text,
            ActivityCategory category,
            Integer price,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        String normalized = utils.cleanText(text);
        Map<PreferenceTagGroup, PreferenceTag> selections = new EnumMap<>(PreferenceTagGroup.class);

        selections.put(PreferenceTagGroup.MOOD, chooseMood(normalized, category));
        selections.put(PreferenceTagGroup.INTENSITY, chooseIntensity(normalized, startAt, endAt));
        selections.put(PreferenceTagGroup.PURPOSE, choosePurpose(normalized, category));
        selections.put(PreferenceTagGroup.DURATION, chooseDuration(normalized, startAt, endAt));
        selections.put(PreferenceTagGroup.SIZE, chooseSize(normalized));

        List<PreferenceTag> tags = new ArrayList<>();
        for (PreferenceTagGroup group : List.of(
                PreferenceTagGroup.MOOD,
                PreferenceTagGroup.INTENSITY,
                PreferenceTagGroup.PURPOSE,
                PreferenceTagGroup.DURATION,
                PreferenceTagGroup.SIZE
        )) {
            PreferenceTag tag = selections.get(group);
            if (tag != null && !tags.contains(tag)) {
                tags.add(tag);
            }
        }

        return tags;
    }

    private PreferenceTag chooseMood(String text, ActivityCategory category) {
        if (contains(text, "차분", "독서", "명상", "조용", "여유", "감성", "쉼", "정리")) {
            return PreferenceTag.CALM;
        }
        if (contains(text, "힐링", "휴식", "치유", "회복", "스트레스")) {
            return PreferenceTag.HEALING;
        }
        if (contains(text, "활기", "러닝", "운동", "댄스", "축제", "파티", "액티브")) {
            return PreferenceTag.LIVELY;
        }
        if (contains(text, "감성", "전시", "사진", "문학", "공연", "아트")) {
            return PreferenceTag.EMOTIONAL;
        }
        if (contains(text, "창의", "만들기", "메이커", "DIY", "그림", "작업", "제작")) {
            return PreferenceTag.CREATIVE;
        }
        if (contains(text, "트렌드", "핫플", "핫", "힙", "인기", "요즘")) {
            return PreferenceTag.TRENDY;
        }

        return switch (category == null ? ActivityCategory.CULTURE : category) {
            case SPORTS, TRAVEL -> PreferenceTag.LIVELY;
            case CULTURE, PHOTO_VIDEO -> PreferenceTag.CREATIVE;
            case LANGUAGE, HUMANITIES, CAREER -> PreferenceTag.CALM;
            case VOLUNTEER -> PreferenceTag.HEALING;
            default -> PreferenceTag.CALM;
        };
    }

    private PreferenceTag chooseIntensity(String text, LocalDateTime startAt, LocalDateTime endAt) {
        long days = durationDays(startAt, endAt);
        if (contains(text, "입문", "초보", "처음", "기초", "원데이")) {
            return PreferenceTag.BEGINNER;
        }
        if (contains(text, "가볍게", "라이트", "부담없", "캐주얼") || (days >= 0 && days <= 1)) {
            return PreferenceTag.LIGHT;
        }
        if (contains(text, "몰입", "심화", "집중", "깊이", "실습")) {
            return PreferenceTag.IMMERSIVE;
        }
        if (contains(text, "도전", "고급", "전문", "챌린지")) {
            return PreferenceTag.CHALLENGE;
        }
        if (days > 31) {
            return PreferenceTag.IMMERSIVE;
        }
        return PreferenceTag.LIGHT;
    }

    private PreferenceTag choosePurpose(String text, ActivityCategory category) {
        if (contains(text, "휴식", "회복", "힐링", "명상", "쉼")) {
            return PreferenceTag.REST;
        }
        if (contains(text, "취미", "원데이", "클래스", "만들기", "체험")) {
            return PreferenceTag.HOBBY;
        }
        if (contains(text, "배움", "강의", "교육", "학습", "수업", "스터디")) {
            return PreferenceTag.LEARNING;
        }
        if (contains(text, "성장", "커리어", "이직", "직무", "취업", "역량")) {
            return PreferenceTag.GROWTH;
        }
        if (contains(text, "모임", "네트워킹", "친목", "소통", "커뮤니티")) {
            return PreferenceTag.SOCIAL;
        }
        if (contains(text, "체험", "탐방", "경험", "방문", "투어")) {
            return PreferenceTag.EXPERIENCE;
        }

        return switch (category == null ? ActivityCategory.CULTURE : category) {
            case VOLUNTEER, CAREER, LANGUAGE -> PreferenceTag.GROWTH;
            case SPORTS, TRAVEL -> PreferenceTag.EXPERIENCE;
            case HUMANITIES -> PreferenceTag.LEARNING;
            default -> PreferenceTag.HOBBY;
        };
    }

    private PreferenceTag chooseDuration(String text, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null) {
            long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
            if (days <= 1) {
                return PreferenceTag.ONE_DAY;
            }
            if (days <= 3) {
                return PreferenceTag.THREE_DAYS;
            }
            if (days <= 7) {
                return PreferenceTag.ONE_WEEK;
            }
            if (days <= 31) {
                return PreferenceTag.ONE_MONTH;
            }
            if (days <= 93) {
                return PreferenceTag.THREE_MONTHS;
            }
            if (days <= 366) {
                return PreferenceTag.OVER_SIX_MONTHS;
            }
            return PreferenceTag.OVER_ONE_YEAR;
        }

        if (contains(text, "원데이", "하루", "당일")) {
            return PreferenceTag.ONE_DAY;
        }
        if (contains(text, "3일", "삼일", "단기")) {
            return PreferenceTag.THREE_DAYS;
        }
        if (contains(text, "1주", "일주일")) {
            return PreferenceTag.ONE_WEEK;
        }
        if (contains(text, "1개월", "한달", "한 달")) {
            return PreferenceTag.ONE_MONTH;
        }
        if (contains(text, "3개월")) {
            return PreferenceTag.THREE_MONTHS;
        }
        return PreferenceTag.ONE_MONTH;
    }

    private PreferenceTag chooseSize(String text) {
        if (contains(text, "대규모", "대형", "수십명", "수백명", "페스티벌", "행사장")) {
            return PreferenceTag.LARGE;
        }
        if (contains(text, "소규모", "소그룹", "1:1", "개인", "클래스", "소수")) {
            return PreferenceTag.SMALL;
        }
        return PreferenceTag.SMALL;
    }

    private boolean contains(String text, String... keywords) {
        String value = text == null ? "" : text;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private long durationDays(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate());
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
