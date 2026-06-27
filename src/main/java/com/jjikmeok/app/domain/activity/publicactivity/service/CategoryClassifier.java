package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class CategoryClassifier {

    private static final int INVALID_FUTURE_YEAR = 2099;

    public ActivityCategory classifyCategory(SourceType sourceType, String text) {
        String value = normalize(text);

        if (sourceType == SourceType.KOPIS) {
            return ActivityCategory.CULTURE;
        }

        if (sourceType == SourceType.EXHIBITION) {
            if (containsAny(value, "사진", "영상", "촬영", "미디어", "필름", "다큐", "포토")) {
                return ActivityCategory.PHOTO_VIDEO;
            }
            if (containsAny(value, "공예", "만들기", "도예", "뜨개", "가죽", "목공", "핸드메이드")) {
                return ActivityCategory.CRAFT;
            }
            return ActivityCategory.CULTURE;
        }

        return classifyByKeywords(value);
    }

    public ActivityType classifyType(SourceType sourceType, String text) {
        return classifyType(sourceType, text, null, null);
    }

    public ActivityType classifyType(SourceType sourceType, String text, LocalDateTime startAt, LocalDateTime endAt) {
        String value = normalize(text);

        if (validDate(startAt) && validDate(endAt)) {
            long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
            if (days > 1) {
                if (containsAny(value, "프로그램", "과정", "코스", "정규", "기수", "클래스", "교육", "스터디")) {
                    return ActivityType.PROGRAM;
                }
                if (containsAny(value, "행사", "공연", "축제", "강연", "전시", "박람회")) {
                    return ActivityType.EVENT;
                }
                return ActivityType.PROGRAM;
            }
            if (days == 1 && containsAny(value, "원데이", "원 데이", "1일", "하루", "체험")) {
                return ActivityType.ONE_DAY;
            }
        }

        if (containsAny(value, "동아리", "모임", "커뮤니티", "크루", "정기모임", "클럽")) {
            return ActivityType.CLUB;
        }
        if (containsAny(value, "프로그램", "과정", "코스", "정규", "기수", "클래스", "교육", "스터디")) {
            return ActivityType.PROGRAM;
        }
        if (containsAny(value, "행사", "공연", "축제", "강연", "전시", "박람회")) {
            return ActivityType.EVENT;
        }
        if (containsAny(value, "원데이", "원 데이", "1일", "하루", "체험")) {
            return ActivityType.ONE_DAY;
        }

        return ActivityType.EVENT;
    }

    private ActivityCategory classifyByKeywords(String value) {
        if (containsAny(value, "요리", "베이킹", "쿠킹", "디저트", "제과", "제빵")) {
            return ActivityCategory.COOKING;
        }
        if (containsAny(value, "공예", "만들기", "도예", "뜨개", "가죽", "목공", "핸드메이드")) {
            return ActivityCategory.CRAFT;
        }
        if (containsAny(value, "운동", "액티비티", "러닝", "등산", "요가", "필라테스", "스포츠", "클라이밍")) {
            return ActivityCategory.SPORTS;
        }
        if (containsAny(value, "사진", "영상", "촬영", "미디어", "필름", "다큐", "포토")) {
            return ActivityCategory.PHOTO_VIDEO;
        }
        if (containsAny(value, "책", "글", "독서", "글쓰기", "작문", "인문", "문학")) {
            return ActivityCategory.HUMANITIES;
        }
        if (containsAny(value, "여행", "탐방", "투어", "트립", "캠프", "답사")) {
            return ActivityCategory.TRAVEL;
        }
        if (containsAny(value, "언어", "영어", "일본어", "중국어", "회화", "글로벌", "해외")) {
            return ActivityCategory.LANGUAGE;
        }
        if (containsAny(value, "봉사", "기부", "나눔", "사회공헌", "자원봉사")) {
            return ActivityCategory.VOLUNTEER;
        }
        if (containsAny(value, "성장", "커리어", "취업", "직무", "브랜딩", "마케팅", "포트폴리오", "창업")) {
            return ActivityCategory.CAREER;
        }
        if (containsAny(value, "문화", "예술", "공연", "전시", "뮤지컬", "연극", "콘서트", "미술")) {
            return ActivityCategory.CULTURE;
        }
        return null;
    }

    private boolean validDate(LocalDateTime value) {
        return value != null && value.getYear() < INVALID_FUTURE_YEAR;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
