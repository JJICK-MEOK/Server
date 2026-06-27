package com.jjikmeok.app.domain.ai.service;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.ai.dto.ExtractedActivityDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryDuration;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryGroupSize;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryIntensity;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryMood;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryPurpose;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryAiAnalysisService {

    private final AiActivityParser aiActivityParser;
    private final ActivitySyncUtils utils;

    public DiscoveryAnalysisDto analyze(DiscoveryCandidateDto candidate) {
        if (candidate == null) {
            return null;
        }

        ExtractedActivityDto ai = aiActivityParser.parseDiscovery(context(candidate));
        DiscoveryCandidateDto merged = merge(candidate, ai);

        String text = utils.cleanText(join(
                merged.keyword(),
                merged.title(),
                merged.description(),
                merged.organizer(),
                merged.contactInfo(),
                merged.target(),
                merged.address(),
                merged.pageText(),
                candidate.searchResult() == null ? null : candidate.searchResult().snippet()
        ));

        ActivityCategory category = firstNonNull(
                classifyCategory(text),
                parseEnum(ai.category(), ActivityCategory.class)
        );
        ActivityType activityType = firstNonNull(
                classifyType(text, merged.startAt(), merged.endAt()),
                parseEnum(ai.activityType(), ActivityType.class)
        );

        List<DiscoveryMood> moods = classifyMoods(text);
        if (moods.size() < 2) {
            addMoodIfMissing(moods, parseEnum(ai.moodTag1(), DiscoveryMood.class));
            addMoodIfMissing(moods, parseEnum(ai.moodTag2(), DiscoveryMood.class));
        }

        DiscoveryIntensity intensity = firstNonNull(
                classifyIntensity(text, merged.startAt(), merged.endAt()),
                parseEnum(ai.intensity(), DiscoveryIntensity.class)
        );
        DiscoveryPurpose purpose = firstNonNull(
                classifyPurpose(text),
                parseEnum(ai.purpose(), DiscoveryPurpose.class)
        );
        DiscoveryDuration duration = firstNonNull(
                classifyDuration(text, merged.startAt(), merged.endAt()),
                parseEnum(ai.duration(), DiscoveryDuration.class)
        );
        DiscoveryGroupSize groupSize = firstNonNull(
                classifyGroupSize(text),
                parseEnum(ai.groupSize(), DiscoveryGroupSize.class)
        );

        return new DiscoveryAnalysisDto(
                merged.keyword(),
                merged.searchResult() == null || merged.searchResult().sourceChannel() == null
                        ? "WEBSITE"
                        : merged.searchResult().sourceChannel().name(),
                merged.title(),
                merged.sourceUrl(),
                merged.thumbnailUrl(),
                merged.description(),
                merged.organizer(),
                merged.contactInfo(),
                merged.target(),
                merged.address(),
                merged.startAt(),
                merged.endAt(),
                merged.recruitStartAt(),
                merged.recruitEndAt(),
                merged.price(),
                category,
                activityType,
                moods.size() > 0 ? moods.get(0) : parseEnum(ai.moodTag1(), DiscoveryMood.class),
                moods.size() > 1 ? moods.get(1) : parseEnum(ai.moodTag2(), DiscoveryMood.class),
                intensity,
                purpose,
                duration,
                groupSize,
                confidenceScore(merged, ai != null),
                candidate.searchResult() == null ? null : candidate.searchResult().snippet(),
                merged.extractionMode()
        );
    }

    private DiscoveryCandidateDto merge(DiscoveryCandidateDto candidate, ExtractedActivityDto ai) {
        if (ai == null) {
            return candidate;
        }

        return new DiscoveryCandidateDto(
                candidate.keyword(),
                candidate.searchResult(),
                first(candidate.title(), candidate.searchResult() == null ? null : candidate.searchResult().title(), ai.title()),
                candidate.sourceUrl(),
                candidate.thumbnailUrl(),
                firstText(ai.description(), candidate.description()),
                firstText(ai.organizer(), candidate.organizer()),
                firstText(ai.contactInfo(), candidate.contactInfo()),
                firstText(ai.target(), candidate.target()),
                firstText(candidate.address(), ai.address()),
                firstDate(candidate.startAt(), ai.startAt()),
                firstDate(candidate.endAt(), ai.endAt()),
                firstDate(candidate.recruitStartAt(), ai.recruitStartAt()),
                firstDate(candidate.recruitEndAt(), ai.recruitEndAt()),
                firstPrice(candidate.price(), ai.price()),
                candidate.extractionMode(),
                candidate.confidenceScore(),
                candidate.pageText()
        );
    }

    private ActivityCategory classifyCategory(String text) {
        if (contains(text, "운동", "스포츠", "액티비티", "러닝", "댄스", "요가")) {
            return ActivityCategory.SPORTS;
        }
        if (contains(text, "문화", "예술", "공연", "전시", "음악", "연극", "콘서트")) {
            return ActivityCategory.CULTURE;
        }
        if (contains(text, "공예", "만들기", "DIY", "메이커", "제작")) {
            return ActivityCategory.CRAFT;
        }
        if (contains(text, "요리", "베이킹", "쿠킹", "조리")) {
            return ActivityCategory.COOKING;
        }
        if (contains(text, "사진", "영상", "촬영", "편집", "미디어")) {
            return ActivityCategory.PHOTO_VIDEO;
        }
        if (contains(text, "책", "글", "독서", "문학", "강연")) {
            return ActivityCategory.HUMANITIES;
        }
        if (contains(text, "여행", "탐방", "투어", "답사", "산책")) {
            return ActivityCategory.TRAVEL;
        }
        if (contains(text, "언어", "영어", "일본어", "중국어", "외국어", "해외")) {
            return ActivityCategory.LANGUAGE;
        }
        if (contains(text, "봉사", "기부", "나눔", "재능기부")) {
            return ActivityCategory.VOLUNTEER;
        }
        if (contains(text, "성장", "커리어", "취업", "이직", "직무", "네트워킹")) {
            return ActivityCategory.CAREER;
        }
        return null;
    }

    private ActivityType classifyType(String text, LocalDateTime startAt, LocalDateTime endAt) {
        if (contains(text, "클럽", "모임", "스터디", "동아리")) {
            return ActivityType.CLUB;
        }
        if (contains(text, "원데이", "하루", "당일")) {
            return ActivityType.ONE_DAY;
        }
        if (contains(text, "프로그램", "강좌", "교육", "수업", "클래스")) {
            return ActivityType.PROGRAM;
        }
        if (contains(text, "공연", "행사", "전시", "축제", "모집")) {
            return ActivityType.EVENT;
        }
        if (startAt != null && endAt != null && ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) <= 0) {
            return ActivityType.ONE_DAY;
        }
        return null;
    }

    private List<DiscoveryMood> classifyMoods(String text) {
        Set<DiscoveryMood> moods = new LinkedHashSet<>();
        addMood(moods, text, DiscoveryMood.CALM, "차분", "조용", "여유", "쉼", "독서", "명상", "산책");
        addMood(moods, text, DiscoveryMood.HEALING, "힐링", "휴식", "치유", "회복");
        addMood(moods, text, DiscoveryMood.LIVELY, "활기", "운동", "댄스", "축제", "액티비티", "러닝");
        addMood(moods, text, DiscoveryMood.EMOTIONAL, "감성", "전시", "사진", "문학", "공연");
        addMood(moods, text, DiscoveryMood.CREATIVE, "창의", "만들기", "메이커", "DIY", "작업", "제작");
        addMood(moods, text, DiscoveryMood.TRENDY, "트렌드", "핫", "인기", "힙");
        return new ArrayList<>(moods);
    }

    private DiscoveryIntensity classifyIntensity(String text, LocalDateTime startAt, LocalDateTime endAt) {
        long days = durationDays(startAt, endAt);
        if (contains(text, "입문", "초보", "처음", "기초")) {
            return DiscoveryIntensity.BEGINNER;
        }
        if (contains(text, "가볍게", "부담없", "라이트") || (days >= 0 && days <= 1)) {
            return DiscoveryIntensity.LIGHT;
        }
        if (contains(text, "몰입", "심화", "집중", "깊이")) {
            return DiscoveryIntensity.IMMERSIVE;
        }
        if (contains(text, "도전", "고급", "챌린지")) {
            return DiscoveryIntensity.CHALLENGE;
        }
        return null;
    }

    private DiscoveryPurpose classifyPurpose(String text) {
        if (contains(text, "휴식", "힐링", "명상", "쉼")) {
            return DiscoveryPurpose.REST;
        }
        if (contains(text, "취미", "클래스", "원데이", "체험", "만들기")) {
            return DiscoveryPurpose.HOBBY;
        }
        if (contains(text, "배움", "교육", "강의", "강좌", "스터디")) {
            return DiscoveryPurpose.LEARNING;
        }
        if (contains(text, "성장", "커리어", "취업", "이직", "직무")) {
            return DiscoveryPurpose.GROWTH;
        }
        return null;
    }

    private DiscoveryDuration classifyDuration(String text, LocalDateTime startAt, LocalDateTime endAt) {
        long days = durationDays(startAt, endAt);
        if (days >= 0 && days <= 7) {
            return DiscoveryDuration.SHORT_TERM;
        }
        if (days > 7 && days <= 31) {
            return DiscoveryDuration.ONE_MONTH;
        }
        if (days > 31 && days <= 183) {
            return DiscoveryDuration.SIX_MONTHS;
        }
        if (days > 183) {
            return DiscoveryDuration.OVER_ONE_YEAR;
        }
        if (contains(text, "단기", "원데이", "하루")) {
            return DiscoveryDuration.SHORT_TERM;
        }
        if (contains(text, "1개월", "한달", "한 달")) {
            return DiscoveryDuration.ONE_MONTH;
        }
        if (contains(text, "6개월", "반년")) {
            return DiscoveryDuration.SIX_MONTHS;
        }
        if (contains(text, "1년", "장기")) {
            return DiscoveryDuration.OVER_ONE_YEAR;
        }
        return null;
    }

    private DiscoveryGroupSize classifyGroupSize(String text) {
        if (contains(text, "대규모", "수백명", "수천명", "페스티벌", "행사장")) {
            return DiscoveryGroupSize.LARGE;
        }
        if (contains(text, "소규모", "소수", "개인", "1:1", "클래스")) {
            return DiscoveryGroupSize.SMALL;
        }
        return null;
    }

    private double confidenceScore(DiscoveryCandidateDto candidate, boolean aiUsed) {
        double score = candidate.confidenceScore();
        if (!utils.isBlank(candidate.title())) score += 10;
        if (!utils.isBlank(candidate.description())) score += 10;
        if (!utils.isBlank(candidate.organizer())) score += 10;
        if (!utils.isBlank(candidate.contactInfo())) score += 10;
        if (!utils.isBlank(candidate.target())) score += 5;
        if (!utils.isBlank(candidate.address())) score += 5;
        if (candidate.startAt() != null) score += 10;
        if (candidate.endAt() != null) score += 10;
        if (candidate.recruitStartAt() != null) score += 5;
        if (candidate.recruitEndAt() != null) score += 5;
        if (candidate.price() != null) score += 5;
        if (aiUsed) score += 5;
        return Math.min(100, score);
    }

    private long durationDays(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return -1;
        }
        return Math.max(0, ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1);
    }

    private String context(DiscoveryCandidateDto candidate) {
        return """
                [keyword]
                %s

                [title]
                %s

                [url]
                %s

                [snippet]
                %s

                [organizer]
                %s

                [contact]
                %s

                [target]
                %s

                [address]
                %s

                [page]
                %s
                """.formatted(
                candidate.keyword(),
                candidate.title(),
                candidate.sourceUrl(),
                candidate.searchResult() == null ? null : candidate.searchResult().snippet(),
                candidate.organizer(),
                candidate.contactInfo(),
                candidate.target(),
                candidate.address(),
                candidate.pageText()
        );
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

    private void addMood(Set<DiscoveryMood> moods, String text, DiscoveryMood mood, String... keywords) {
        if (contains(text, keywords)) {
            moods.add(mood);
        }
    }

    private void addMoodIfMissing(List<DiscoveryMood> moods, DiscoveryMood mood) {
        if (mood != null && !moods.contains(mood) && moods.size() < 2) {
            moods.add(mood);
        }
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (utils.isBlank(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private LocalDateTime firstDate(LocalDateTime existing, LocalDateTime incoming) {
        return existing != null ? existing : incoming;
    }

    private Integer firstPrice(Integer existing, Integer incoming) {
        return existing != null ? existing : incoming;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalized)) {
                return constant;
            }
        }
        return null;
    }

    private <T> T firstNonNull(T left, T right) {
        return left != null ? left : right;
    }
}
