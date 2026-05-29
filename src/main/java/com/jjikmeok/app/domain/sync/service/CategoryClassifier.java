package com.jjikmeok.app.domain.sync.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class CategoryClassifier {

    private static final int INVALID_FUTURE_YEAR = 2099;

    public ActivityCategory classifyCategory(SourceType sourceType, String text) {
        String value = text == null ? "" : text;

        if (sourceType == SourceType.VOLUNTEER_1365 || contains(value, "봉사", "자원봉사")) return ActivityCategory.VOLUNTEER;

        if (sourceType == SourceType.KOPIS) {
            if (contains(value, "음악", "콘서트", "국악")) return ActivityCategory.MUSIC;
            if (contains(value, "무용", "댄스")) return ActivityCategory.DANCE;
            return ActivityCategory.CULTURE;
        }

        if (sourceType == SourceType.EXHIBITION) {
            if (contains(value, "사진", "영상")) return ActivityCategory.PHOTO_VIDEO;
            if (contains(value, "공예", "도예")) return ActivityCategory.CRAFT;
            return ActivityCategory.CULTURE;
        }

        if (sourceType == SourceType.TOUR_API || sourceType == SourceType.SEOUL_RESERVATION) {
            if (contains(value, "스포츠", "레포츠", "축구", "야구", "농구", "테니스")) return ActivityCategory.SPORTS;
            if (contains(value, "요리", "푸드", "쿠킹", "베이킹")) return ActivityCategory.COOKING;
            if (contains(value, "공예", "도자", "만들기")) return ActivityCategory.CRAFT;
            if (contains(value, "전시", "관람", "공연", "문화", "박물관", "미술관", "역사", "해설", "투어", "스타디움")) return ActivityCategory.CULTURE;
            if (contains(value, "여행", "캠핑", "관광지", "숙박")) return ActivityCategory.TRAVEL;
            return ActivityCategory.ETC;
        }

        if (sourceType == SourceType.SEOUL_CULTURE && contains(value, "북토크", "독서", "인문학")) return ActivityCategory.HUMANITIES;

        if (sourceType == SourceType.YOUTH_CONTENT) {
            if (contains(value, "취업", "직업훈련", "채용", "면접", "자격증", "실무", "커리어", "일경험")) return ActivityCategory.CAREER;
            if (contains(value, "교육", "강의", "클래스", "과정", "멘토링", "훈련", "아카데미")) return ActivityCategory.SELF_DEVELOPMENT;
        }

        if (contains(value, "요리", "베이킹", "쿠킹")) return ActivityCategory.COOKING;
        if (contains(value, "공예", "도자", "만들기")) return ActivityCategory.CRAFT;
        if (contains(value, "운동", "스포츠", "체육", "러닝", "등산")) return ActivityCategory.SPORTS;
        if (contains(value, "공연", "축제", "전시", "문화", "강연", "투어", "해설", "스타디움")) return ActivityCategory.CULTURE;
        if (contains(value, "음악", "콘서트", "국악")) return ActivityCategory.MUSIC;
        if (contains(value, "무용", "댄스")) return ActivityCategory.DANCE;
        if (contains(value, "북토크", "독서", "인문학")) return ActivityCategory.HUMANITIES;
        if (contains(value, "여행", "캠핑", "관광지", "숙박")) return ActivityCategory.TRAVEL;

        return ActivityCategory.ETC;
    }

    public ActivityType classifyType(SourceType sourceType, String text) {
        return classifyType(sourceType, text, null, null);
    }

    public ActivityType classifyType(SourceType sourceType, String text, LocalDateTime startAt, LocalDateTime endAt) {
        String value = text == null ? "" : text;

        if (validDate(startAt) && validDate(endAt)) {
            long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
            if (days > 1) {
                return contains(value, "공연", "전시", "축제", "콘서트", "뮤지컬", "페스티벌", "팝업", "행사", "이벤트") ? ActivityType.EVENT : ActivityType.PROGRAM;
            }
            if (days == 1 && (sourceType == SourceType.SEOUL_RESERVATION || contains(value, "원데이", "하루", "1회", "일일", "당일", "체험"))) return ActivityType.ONE_DAY;
        }

        if (contains(value, "클럽", "크루", "모임", "동아리", "소모임")) return ActivityType.CLUB;
        if (contains(value, "강좌", "교육", "과정", "클래스", "멘토링", "훈련", "아카데미", "수업", "프로그램")) return ActivityType.PROGRAM;
        if (contains(value, "행사", "공연", "전시", "축제", "강연", "콘서트", "뮤지컬", "페스티벌", "팝업", "이벤트")) return ActivityType.EVENT;
        if (contains(value, "원데이", "체험", "하루", "1회", "원데이클래스", "일일", "당일")) return ActivityType.ONE_DAY;

        return (sourceType == SourceType.VOLUNTEER_1365 || sourceType == SourceType.YOUTH_CONTENT) ? ActivityType.PROGRAM : ActivityType.EVENT;
    }

    private boolean validDate(LocalDateTime value) {
        return value != null && value.getYear() < INVALID_FUTURE_YEAR;
    }

    private boolean contains(String value, String... keywords) {
        for (String k : keywords) {
            if (value.contains(k)) return true;
        }
        return false;
    }
}
