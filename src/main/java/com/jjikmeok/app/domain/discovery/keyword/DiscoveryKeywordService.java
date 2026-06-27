package com.jjikmeok.app.domain.discovery.keyword;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class DiscoveryKeywordService {

    private static final Map<ActivityCategory, List<String>> DEFAULT_KEYWORDS = buildDefaults();

    public List<ActivityCategory> categories() {
        List<ActivityCategory> categories = new ArrayList<>();
        Collections.addAll(categories, ActivityCategory.values());
        return categories;
    }

    public List<String> keywordsFor(ActivityCategory category, int limit) {
        List<String> keywords = DEFAULT_KEYWORDS.getOrDefault(category, List.of(category == null ? "활동" : category.getLabel()));
        if (limit <= 0 || limit >= keywords.size()) {
            return keywords;
        }
        return keywords.subList(0, limit);
    }

    public Map<ActivityCategory, List<String>> snapshot() {
        return Collections.unmodifiableMap(DEFAULT_KEYWORDS);
    }

    private static Map<ActivityCategory, List<String>> buildDefaults() {
        Map<ActivityCategory, List<String>> keywords = new EnumMap<>(ActivityCategory.class);
        keywords.put(ActivityCategory.SPORTS, expand(
                new String[]{"운동모임", "등산모임", "러닝클럽", "마라톤", "축구", "야구", "테니스", "요가"},
                new String[]{" 모집", " 정기 모집", " 원데이 클래스", " 체험", " 참가자 모집", " 동호회", " 모임", " 클래스"}));
        keywords.put(ActivityCategory.CULTURE, expand(
                new String[]{"전시", "공연", "축제", "강연", "문화행사", "아트", "뮤지컬", "콘서트"},
                new String[]{" 신청", " 모집", " 참가", " 안내", " 추천", " 체험", " 예약", " 무료"}));
        keywords.put(ActivityCategory.CRAFT, expand(
                new String[]{"공예", "도자기", "만들기공예", "목공", "가죽공예", "비누공예", "캔들", "뜨개"},
                new String[]{" 클래스", " 원데이 클래스", " 체험", " 모집", " 워크숍", " 교육", " 과정", " 모임"}));
        keywords.put(ActivityCategory.COOKING, expand(
                new String[]{"요리", "베이킹", "쿠킹", "제빵", "디저트", "브런치", "커피", "채식"},
                new String[]{" 클래스", " 모집", " 원데이 클래스", " 체험", " 교육", " 워크숍", " 실습", " 과정"}));
        keywords.put(ActivityCategory.PHOTO_VIDEO, expand(
                new String[]{"사진", "영상", "촬영", "편집", "카메라", "브이로그", "쇼츠", "크리에이터"},
                new String[]{" 클래스", " 모집", " 워크숍", " 체험", " 교육", " 실무", " 기초반", " 세미나"}));
        keywords.put(ActivityCategory.HUMANITIES, expand(
                new String[]{"북토크", "독서모임", "인문학", "철학", "글쓰기", "문학", "에세이", "서평"},
                new String[]{" 모집", " 모임", " 클래스", " 강연", " 세미나", " 워크숍", " 신청", " 북클럽"}));
        keywords.put(ActivityCategory.TRAVEL, expand(
                new String[]{"여행", "탐방", "답사", "캠핑", "트레킹", "문화탐방", "도보", "투어"},
                new String[]{" 모집", " 체험", " 안내", " 신청", " 동행", " 코스", " 프로그램", " 클래스"}));
        keywords.put(ActivityCategory.LANGUAGE, expand(
                new String[]{"영어회화", "일본어", "중국어", "회화", "어학", "스피킹", "토익", "유학"},
                new String[]{" 클래스", " 모집", " 스터디", " 모임", " 원데이", " 교육", " 회화반", " 특강"}));
        keywords.put(ActivityCategory.VOLUNTEER, expand(
                new String[]{"봉사", "자원봉사", "환경", "아동", "노인", "유기견", "플로깅", "캠페인"},
                new String[]{" 모집", " 활동", " 프로그램", " 참여자 모집", " 봉사단", " 정기봉사", " 캠페인", " 행사"}));
        keywords.put(ActivityCategory.CAREER, expand(
                new String[]{"취업", "채용", "커리어", "직무", "실무", "포트폴리오", "면접", "멘토링"},
                new String[]{" 모집", " 컨설팅", " 특강", " 코칭", " 스터디", " 교육", " 세미나", " 프로그램"}));
        return keywords;
    }

    private static List<String> expand(String[] stems, String[] suffixes) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String stem : stems) {
            for (String suffix : suffixes) {
                values.add(stem + suffix);
            }
        }
        return List.copyOf(values);
    }
}
