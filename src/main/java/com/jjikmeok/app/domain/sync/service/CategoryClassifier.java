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

        if (sourceType == SourceType.KOPIS) {
            return ActivityCategory.CULTURE;
        }

        if (sourceType == SourceType.EXHIBITION) {
            if (contains(value, "사진", "영상", "촬영", "필름", "미디어")) return ActivityCategory.PHOTO_VIDEO;
            if (contains(value, "공예", "만들기", "도예", "목공", "뜨개", "캔들")) return ActivityCategory.CRAFT;
            return ActivityCategory.CULTURE;
        }

        if (sourceType == SourceType.SEOUL_RESERVATION) {
            if (contains(value, "스포츠", "운동", "액티비티", "축구", "야구", "테니스", "러닝", "등산", "요가")) return ActivityCategory.SPORTS;
            if (contains(value, "요리", "베이킹", "쿠킹", "제과", "브런치")) return ActivityCategory.COOKING;
            if (contains(value, "공예", "만들기", "도예", "뜨개", "목공")) return ActivityCategory.CRAFT;
            if (contains(value, "여행", "탐방", "투어", "캠핑", "트레킹")) return ActivityCategory.TRAVEL;
            if (contains(value, "독서", "북토크", "문학", "철학", "글쓰기")) return ActivityCategory.HUMANITIES;
            if (contains(value, "봉사", "자원봉사", "플로깅", "기부")) return ActivityCategory.VOLUNTEER;
            if (contains(value, "영어", "일본어", "중국어", "회화", "유학")) return ActivityCategory.LANGUAGE;
            if (contains(value, "취업", "채용", "커리어", "멘토링", "세미나", "교육", "클래스", "워크숍")) return ActivityCategory.CAREER;
            return ActivityCategory.CULTURE;
        }

        if (contains(value, "요리", "베이킹", "쿠킹", "제과", "디저트")) return ActivityCategory.COOKING;
        if (contains(value, "공예", "만들기", "도예", "뜨개", "목공", "캔들")) return ActivityCategory.CRAFT;
        if (contains(value, "운동", "스포츠", "액티비티", "축구", "야구", "테니스", "러닝", "등산")) return ActivityCategory.SPORTS;
        if (contains(value, "사진", "영상", "촬영", "카메라", "편집", "브이로그")) return ActivityCategory.PHOTO_VIDEO;
        if (contains(value, "독서", "북토크", "문학", "철학", "글쓰기", "에세이")) return ActivityCategory.HUMANITIES;
        if (contains(value, "여행", "탐방", "투어", "캠핑", "트레킹", "답사")) return ActivityCategory.TRAVEL;
        if (contains(value, "영어", "일본어", "중국어", "회화", "어학", "유학")) return ActivityCategory.LANGUAGE;
        if (contains(value, "봉사", "자원봉사", "플로깅", "환경", "기부")) return ActivityCategory.VOLUNTEER;
        if (contains(value, "취업", "채용", "커리어", "직무", "멘토링", "교육", "클래스", "워크숍", "세미나", "스터디")) {
            return ActivityCategory.CAREER;
        }
        if (contains(value, "전시", "축제", "행사", "강연", "문화", "예술", "공연", "콘서트", "오페라", "뮤지컬", "연극", "무용", "댄스", "발레")) {
            return ActivityCategory.CULTURE;
        }

        return ActivityCategory.CULTURE;
    }

    public ActivityType classifyType(SourceType sourceType, String text) {
        return classifyType(sourceType, text, null, null);
    }

    public ActivityType classifyType(SourceType sourceType, String text, LocalDateTime startAt, LocalDateTime endAt) {
        String value = text == null ? "" : text;

        if (validDate(startAt) && validDate(endAt)) {
            long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
            if (days > 1) {
                return contains(value, "공연", "전시", "축제", "강연", "워크숍", "세미나", "교육", "프로그램", "행사")
                        ? ActivityType.EVENT
                        : ActivityType.PROGRAM;
            }
            if (days == 1 && contains(value, "원데이", "1일", "하루", "체험", "클래스")) {
                return ActivityType.ONE_DAY;
            }
        }

        if (contains(value, "동아리", "소모임", "모임", "스터디", "클럽")) return ActivityType.CLUB;
        if (contains(value, "교육", "강의", "클래스", "과정", "워크숍", "멘토링", "아카데미")) return ActivityType.PROGRAM;
        if (contains(value, "공연", "전시", "축제", "행사", "강연", "콘서트")) return ActivityType.EVENT;
        if (contains(value, "원데이", "체험", "1일", "하루")) return ActivityType.ONE_DAY;

        return ActivityType.EVENT;
    }

    private boolean validDate(LocalDateTime value) {
        return value != null && value.getYear() < INVALID_FUTURE_YEAR;
    }

    private boolean contains(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
