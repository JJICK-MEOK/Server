package com.jjikmeok.app.domain.activity.privateactivity.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryDuration;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryGroupSize;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryIntensity;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryMood;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryPurpose;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record DiscoverySheetRowDto(
        int rowNumber,
        String reviewer,
        DiscoverySheetStatus status,
        LocalDate createdAt,
        LocalDate publishedAt,
        String keyword,
        String sourceName,
        String title,
        String sourceUrl,
        String thumbnailUrl,
        ActivityType activityType,
        ActivityCategory category,
        LocalDate startAt,
        LocalDate endAt,
        LocalDate recruitStartAt,
        LocalDate recruitEndAt,
        String target,
        Integer price,
        String description,
        String contactInfo,
        String organizer,
        String regionName,
        String address,
        DiscoveryMood moodTag1,
        DiscoveryMood moodTag2,
        DiscoveryIntensity intensity,
        DiscoveryPurpose purpose,
        DiscoveryDuration duration,
        DiscoveryGroupSize groupSize,
        Double confidenceScore,
        String searchSnippet
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static DiscoverySheetRowDto fromCandidate(DiscoveryCandidateDto candidate, int rowNumber, LocalDate createdAt) {
        return fromCandidate(candidate, rowNumber, createdAt, null);
    }

    public static DiscoverySheetRowDto fromCandidate(DiscoveryCandidateDto candidate, int rowNumber, LocalDate createdAt, String regionName) {
        boolean urlOnly = candidate != null && candidate.extractionMode() == ExtractionMode.URL_ONLY;
        return new DiscoverySheetRowDto(
                rowNumber,
                null,
                DiscoverySheetStatus.PENDING,
                createdAt,
                null,
                urlOnly ? null : candidate.keyword(),
                urlOnly ? null : displayOrganizer(candidate.organizer(), candidate.title(), candidate.title()),
                urlOnly ? null : candidate.title(),
                candidate.sourceUrl(),
                urlOnly ? null : candidate.thumbnailUrl(),
                null,
                null,
                candidate.startAt() == null ? null : candidate.startAt().toLocalDate(),
                candidate.endAt() == null ? null : candidate.endAt().toLocalDate(),
                candidate.recruitStartAt() == null ? null : candidate.recruitStartAt().toLocalDate(),
                candidate.recruitEndAt() == null ? null : candidate.recruitEndAt().toLocalDate(),
                candidate.target(),
                candidate.price(),
                candidate.description(),
                candidate.contactInfo(),
                displayOrganizer(candidate.organizer(), candidate.title(), candidate.title()),
                regionName,
                candidate.address(),
                null,
                null,
                null,
                null,
                null,
                null,
                urlOnly ? null : candidate.confidenceScore(),
                urlOnly ? null : candidate.searchResult() == null ? null : candidate.searchResult().snippet()
        );
    }

    public static DiscoverySheetRowDto from(DiscoveryAnalysisDto analysis, int rowNumber, LocalDate createdAt) {
        return from(analysis, rowNumber, createdAt, null);
    }

    public static DiscoverySheetRowDto from(DiscoveryAnalysisDto analysis, int rowNumber, LocalDate createdAt, String regionName) {
        return new DiscoverySheetRowDto(
                rowNumber,
                null,
                DiscoverySheetStatus.PENDING,
                createdAt,
                null,
                analysis.keyword(),
                displayOrganizer(analysis.organizer(), analysis.title(), analysis.title()),
                analysis.title(),
                analysis.sourceUrl(),
                analysis.thumbnailUrl(),
                analysis.activityType(),
                analysis.category(),
                analysis.startAt() == null ? null : analysis.startAt().toLocalDate(),
                analysis.endAt() == null ? null : analysis.endAt().toLocalDate(),
                analysis.recruitStartAt() == null ? null : analysis.recruitStartAt().toLocalDate(),
                analysis.recruitEndAt() == null ? null : analysis.recruitEndAt().toLocalDate(),
                analysis.target(),
                analysis.price(),
                analysis.description(),
                analysis.contactInfo(),
                displayOrganizer(analysis.organizer(), analysis.title(), analysis.title()),
                regionName,
                analysis.address(),
                analysis.moodTag1(),
                analysis.moodTag2(),
                analysis.intensity(),
                analysis.purpose(),
                analysis.duration(),
                analysis.groupSize(),
                analysis.confidenceScore(),
                analysis.searchSnippet()
        );
    }

    public static DiscoverySheetRowDto fromPublicActivity(NormalizedActivity activity, int rowNumber, LocalDate createdAt) {
        return fromPublicActivity(activity, rowNumber, createdAt, null, null);
    }

    public static DiscoverySheetRowDto fromPublicActivity(NormalizedActivity activity, int rowNumber, LocalDate createdAt, String regionName) {
        return fromPublicActivity(activity, rowNumber, createdAt, regionName, null);
    }

    public static DiscoverySheetRowDto fromPublicActivity(
            NormalizedActivity activity,
            int rowNumber,
            LocalDate createdAt,
            String regionName,
            SourceType sourceType
    ) {
        return new DiscoverySheetRowDto(
                rowNumber,
                null,
                DiscoverySheetStatus.PENDING,
                createdAt,
                null,
                sourceType == null ? null : sourceType.name(),
                displayOrganizer(activity.organizer(), activity.title(), activity.title()),
                activity.title(),
                activity.sourceUrl(),
                activity.thumbnailUrl(),
                activity.activityType(),
                activity.category(),
                activity.startAt() == null ? null : activity.startAt().toLocalDate(),
                activity.endAt() == null ? null : activity.endAt().toLocalDate(),
                activity.recruitStartAt() == null ? null : activity.recruitStartAt().toLocalDate(),
                activity.recruitEndAt() == null ? null : activity.recruitEndAt().toLocalDate(),
                activity.target(),
                activity.price(),
                activity.description(),
                activity.contactInfo(),
                displayOrganizer(activity.organizer(), activity.title(), activity.title()),
                regionName,
                activity.address(),
                null,
                null,
                null,
                null,
                null,
                null,
                100d,
                null
        );
    }

    public DiscoverySheetRowDto withAnalysis(DiscoveryAnalysisDto analysis) {
        if (analysis == null) {
            return this;
        }

        return new DiscoverySheetRowDto(
                rowNumber,
                reviewer,
                status,
                createdAt,
                publishedAt,
                firstText(keyword, analysis.keyword()),
                displayOrganizer(sourceName, analysis.organizer(), analysis.title()),
                firstText(title, analysis.title()),
                firstText(sourceUrl, analysis.sourceUrl()),
                firstText(thumbnailUrl, analysis.thumbnailUrl()),
                activityType != null ? activityType : analysis.activityType(),
                category != null ? category : analysis.category(),
                firstDate(startAt, analysis.startAt() == null ? null : analysis.startAt().toLocalDate()),
                firstDate(endAt, analysis.endAt() == null ? null : analysis.endAt().toLocalDate()),
                firstDate(recruitStartAt, analysis.recruitStartAt() == null ? null : analysis.recruitStartAt().toLocalDate()),
                firstDate(recruitEndAt, analysis.recruitEndAt() == null ? null : analysis.recruitEndAt().toLocalDate()),
                firstText(target, analysis.target()),
                price != null ? price : analysis.price(),
                firstText(description, analysis.description()),
                firstText(contactInfo, analysis.contactInfo()),
                displayOrganizer(organizer, analysis.organizer(), analysis.title()),
                regionName,
                firstText(address, analysis.address()),
                moodTag1 != null ? moodTag1 : analysis.moodTag1(),
                moodTag2 != null ? moodTag2 : analysis.moodTag2(),
                intensity != null ? intensity : analysis.intensity(),
                purpose != null ? purpose : analysis.purpose(),
                duration != null ? duration : analysis.duration(),
                groupSize != null ? groupSize : analysis.groupSize(),
                confidenceScore != null ? confidenceScore : analysis.confidenceScore(),
                firstText(searchSnippet, analysis.searchSnippet())
        );
    }

    public DiscoverySheetRowDto withStatus(DiscoverySheetStatus status, LocalDate publishedAt) {
        return new DiscoverySheetRowDto(
                rowNumber,
                reviewer,
                status,
                createdAt,
                publishedAt,
                keyword,
                sourceName,
                title,
                sourceUrl,
                thumbnailUrl,
                activityType,
                category,
                startAt,
                endAt,
                recruitStartAt,
                recruitEndAt,
                target,
                price,
                description,
                contactInfo,
                organizer,
                regionName,
                address,
                moodTag1,
                moodTag2,
                intensity,
                purpose,
                duration,
                groupSize,
                confidenceScore,
                searchSnippet
        );
    }

    public boolean isPublicApiActivity() {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        try {
            return SourceType.valueOf(keyword.trim().toUpperCase(Locale.ROOT)).isPublicApiSource();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<Object> toSheetRow() {
        List<Object> values = new ArrayList<>();
        values.add(rowNumber);
        values.add(format(createdAt));
        values.add(format(publishedAt));
        values.add(reviewer);
        values.add(enumLabel(status == null ? DiscoverySheetStatus.PENDING : status));
        values.add(enumLabel(category));
        values.add(enumLabel(activityType));
        values.add(title);
        values.add(sourceName);
        values.add(sourceUrl);
        values.add(thumbnailUrl);
        values.add(format(startAt));
        values.add(format(endAt));
        values.add(format(recruitStartAt));
        values.add(format(recruitEndAt));
        values.add(target);
        values.add(price);
        values.add(description);
        values.add(contactInfo);
        values.add(regionName);
        values.add(address);
        values.add(enumLabel(moodTag1));
        values.add(enumLabel(moodTag2));
        values.add(enumLabel(intensity));
        values.add(enumLabel(purpose));
        values.add(enumLabel(duration));
        values.add(enumLabel(groupSize));
        values.add(sheetOrigin());
        values.add(confidenceScore);
        return values;
    }

    private String sheetOrigin() {
        if (isPublicApiActivity()) {
            return keyword.trim().toUpperCase(Locale.ROOT);
        }
        return SourceType.DISCOVERY.name();
    }

    public static DiscoverySheetRowDto fromSheetRow(int sheetRowNumber, List<Object> values) {
        Integer number = parseInteger(text(values, 0));
        return new DiscoverySheetRowDto(
                number == null ? sheetRowNumber : number,
                text(values, 3),
                parseStatus(text(values, 4)),
                parseDate(text(values, 1)),
                parseDate(text(values, 2)),
                text(values, 27),
                displayOrganizer(text(values, 8), null, text(values, 7)),
                text(values, 7),
                text(values, 9),
                text(values, 10),
                parseEnum(text(values, 6), ActivityType.class),
                parseActivityCategory(text(values, 5)),
                parseDate(text(values, 11)),
                parseDate(text(values, 12)),
                parseDate(text(values, 13)),
                parseDate(text(values, 14)),
                text(values, 15),
                parseInteger(text(values, 16)),
                text(values, 17),
                text(values, 18),
                text(values, 8),
                text(values, 19),
                text(values, 20),
                parseEnum(text(values, 21), DiscoveryMood.class),
                parseEnum(text(values, 22), DiscoveryMood.class),
                parseEnum(text(values, 23), DiscoveryIntensity.class),
                parseEnum(text(values, 24), DiscoveryPurpose.class),
                parseEnum(text(values, 25), DiscoveryDuration.class),
                parseEnum(text(values, 26), DiscoveryGroupSize.class),
                parseDouble(text(values, 28)),
                null
        );
    }

    public static String[] sheetHeaders() {
        return new String[] {
                "번호",
                "수집 일시",
                "발행 일시",
                "검수자",
                "상태",
                "주제 카테고리",
                "활동 분야",
                "활동명",
                "운영처",
                "링크 URL",
                "썸네일 URL",
                "활동 기간 시작",
                "활동 기간 종료",
                "모집 기간 시작",
                "모집 기간 종료",
                "대상",
                "비용",
                "설명",
                "문의처",
                "지역",
                "장소",
                "분위기 태그 1",
                "분위기 태그 2",
                "강도 태그",
                "목적 태그",
                "기간 태그",
                "규모 태그",
                "원본",
                "신뢰도"
        };
    }

    private static DiscoverySheetStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return DiscoverySheetStatus.PENDING;
        }
        String trimmed = value.trim();
        try {
            return DiscoverySheetStatus.valueOf(trimmed.toUpperCase());
        } catch (Exception e) {
            for (DiscoverySheetStatus status : DiscoverySheetStatus.values()) {
                if (status.getLabel().equalsIgnoreCase(trimmed)) {
                    return status;
                }
            }
            return DiscoverySheetStatus.PENDING;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String format(LocalDate value) {
        return value == null ? null : FORMATTER.format(value);
    }

    private static String text(List<Object> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        Object value = values.get(index);
        return value == null ? null : value.toString();
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static ActivityCategory parseActivityCategory(String value) {
        return parseEnum(value, ActivityCategory.class);
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
            String label = enumLabel(constant);
            if (label != null && label.equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        return null;
    }

    private static LocalDate firstDate(LocalDate first, LocalDate second) {
        return first != null ? first : second;
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String firstText(String first, String second, String third) {
        String value = firstText(first, second);
        return value != null && !value.isBlank() ? value : third;
    }

    private static String displayOrganizer(String first, String second, String title) {
        String normalizedFirst = normalizeDisplayText(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }

        String normalizedSecond = normalizeDisplayText(second);
        if (normalizedSecond != null) {
            return normalizedSecond;
        }

        return normalizeDisplayText(title);
    }

    private static String normalizeDisplayText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (isPublicSourceName(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static boolean isPublicSourceName(String value) {
        return "KOPIS".equals(value)
                || "EXHIBITION".equals(value)
                || "SEOUL_CULTURE".equals(value)
                || "SEOUL_RESERVATION".equals(value)
                || "WEBSITE".equals(value)
                || "BAND".equals(value)
                || "NAVER_CAFE".equals(value)
                || "NAVER_BLOG".equals(value)
                || "BRUNCH".equals(value)
                || "TISTORY".equals(value)
                || "NOTION".equals(value);
    }

    private static String enumLabel(Enum<?> value) {
        if (value == null) {
            return null;
        }
        try {
            Method method = value.getClass().getMethod("getLabel");
            Object label = method.invoke(value);
            return label == null ? null : label.toString();
        } catch (Exception e) {
            return value.name();
        }
    }
}
