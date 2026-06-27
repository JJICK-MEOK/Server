package com.jjikmeok.app.domain.activity.privateactivity.dto.response;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryDuration;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryGroupSize;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryIntensity;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryMood;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoveryPurpose;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySheetStatus;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySourceChannel;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record DiscoverySheetRowDto(
        int rowNumber,
        DiscoverySheetStatus status,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        String keyword,
        String sourceName,
        String title,
        String sourceUrl,
        String thumbnailUrl,
        ActivityType activityType,
        ActivityCategory category,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        LocalDateTime recruitEndAt,
        String target,
        Integer price,
        String description,
        String contactInfo,
        String organizer,
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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static DiscoverySheetRowDto fromCandidate(DiscoveryCandidateDto candidate, int rowNumber, LocalDateTime createdAt) {
        return new DiscoverySheetRowDto(
                rowNumber,
                DiscoverySheetStatus.PENDING,
                createdAt,
                null,
                candidate.keyword(),
                candidate.searchResult() == null || candidate.searchResult().sourceChannel() == null
                        ? DiscoverySourceChannel.WEBSITE.name()
                        : candidate.searchResult().sourceChannel().name(),
                candidate.title(),
                candidate.sourceUrl(),
                candidate.thumbnailUrl(),
                null,
                null,
                candidate.startAt(),
                candidate.endAt(),
                candidate.recruitStartAt(),
                candidate.recruitEndAt(),
                candidate.target(),
                candidate.price(),
                candidate.description(),
                candidate.contactInfo(),
                candidate.organizer(),
                candidate.address(),
                null,
                null,
                null,
                null,
                null,
                null,
                candidate.confidenceScore(),
                candidate.searchResult() == null ? null : candidate.searchResult().snippet()
        );
    }

    public static DiscoverySheetRowDto from(DiscoveryAnalysisDto analysis, int rowNumber, LocalDateTime createdAt) {
        return new DiscoverySheetRowDto(
                rowNumber,
                DiscoverySheetStatus.PENDING,
                createdAt,
                null,
                analysis.keyword(),
                analysis.sourceName(),
                analysis.title(),
                analysis.sourceUrl(),
                analysis.thumbnailUrl(),
                analysis.activityType(),
                analysis.category(),
                analysis.startAt(),
                analysis.endAt(),
                analysis.recruitStartAt(),
                analysis.recruitEndAt(),
                analysis.target(),
                analysis.price(),
                analysis.description(),
                analysis.contactInfo(),
                analysis.organizer(),
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

    public static DiscoverySheetRowDto fromPublicActivity(NormalizedActivity activity, int rowNumber, LocalDateTime createdAt) {
        return new DiscoverySheetRowDto(
                rowNumber,
                DiscoverySheetStatus.PUBLISHED,
                createdAt,
                createdAt,
                null,
                activity.sourceType() == null ? null : activity.sourceType().name(),
                activity.title(),
                activity.sourceUrl(),
                activity.thumbnailUrl(),
                activity.activityType(),
                activity.category(),
                activity.startAt(),
                activity.endAt(),
                activity.recruitStartAt(),
                activity.recruitEndAt(),
                activity.target(),
                activity.price(),
                activity.description(),
                activity.contactInfo(),
                activity.organizer(),
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
                status,
                createdAt,
                publishedAt,
                firstText(keyword, analysis.keyword()),
                firstText(sourceName, analysis.sourceName()),
                firstText(title, analysis.title()),
                firstText(sourceUrl, analysis.sourceUrl()),
                firstText(thumbnailUrl, analysis.thumbnailUrl()),
                activityType != null ? activityType : analysis.activityType(),
                category != null ? category : analysis.category(),
                firstDate(startAt, analysis.startAt()),
                firstDate(endAt, analysis.endAt()),
                firstDate(recruitStartAt, analysis.recruitStartAt()),
                firstDate(recruitEndAt, analysis.recruitEndAt()),
                firstText(target, analysis.target()),
                price != null ? price : analysis.price(),
                firstText(description, analysis.description()),
                firstText(contactInfo, analysis.contactInfo()),
                firstText(organizer, analysis.organizer()),
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

    public DiscoverySheetRowDto withStatus(DiscoverySheetStatus status, LocalDateTime publishedAt) {
        return new DiscoverySheetRowDto(
                rowNumber,
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

    public List<Object> toSheetRow() {
        List<Object> values = new ArrayList<>();
        values.add(rowNumber);
        values.add(status == null ? DiscoverySheetStatus.PENDING.name() : status.name());
        values.add(format(createdAt));
        values.add(format(publishedAt));
        values.add(keyword);
        values.add(sourceName);
        values.add(title);
        values.add(sourceUrl);
        values.add(thumbnailUrl);
        values.add(enumLabel(activityType));
        values.add(enumLabel(category));
        values.add(format(startAt));
        values.add(format(endAt));
        values.add(format(recruitStartAt));
        values.add(format(recruitEndAt));
        values.add(target);
        values.add(price);
        values.add(description);
        values.add(contactInfo);
        values.add(organizer);
        values.add(address);
        values.add(enumLabel(moodTag1));
        values.add(enumLabel(moodTag2));
        values.add(enumLabel(intensity));
        values.add(enumLabel(purpose));
        values.add(enumLabel(duration));
        values.add(enumLabel(groupSize));
        values.add(confidenceScore);
        values.add(searchSnippet);
        return values;
    }

    public static DiscoverySheetRowDto fromSheetRow(int rowNumber, List<Object> values) {
        return new DiscoverySheetRowDto(
                rowNumber,
                parseStatus(text(values, 1)),
                parseDateTime(text(values, 2)),
                parseDateTime(text(values, 3)),
                text(values, 4),
                text(values, 5),
                text(values, 6),
                text(values, 7),
                text(values, 8),
                parseEnum(text(values, 9), ActivityType.class),
                parseActivityCategory(text(values, 10)),
                parseDateTime(text(values, 11)),
                parseDateTime(text(values, 12)),
                parseDateTime(text(values, 13)),
                parseDateTime(text(values, 14)),
                text(values, 15),
                parseInteger(text(values, 16)),
                text(values, 17),
                text(values, 18),
                text(values, 19),
                text(values, 20),
                parseEnum(text(values, 21), DiscoveryMood.class),
                parseEnum(text(values, 22), DiscoveryMood.class),
                parseEnum(text(values, 23), DiscoveryIntensity.class),
                parseEnum(text(values, 24), DiscoveryPurpose.class),
                parseEnum(text(values, 25), DiscoveryDuration.class),
                parseEnum(text(values, 26), DiscoveryGroupSize.class),
                parseDouble(text(values, 27)),
                text(values, 28)
        );
    }

    public static String[] sheetHeaders() {
        return new String[] {
                "ID", "상태", "수집일", "확인일", "수집 키워드", "운영처 / 플랫폼", "활동명", "링크", "썸네일",
                "활동 분야", "주제 카테고리", "활동기간 시작", "활동기간 종료", "모집기간 시작", "모집기간 종료",
                "대상", "금액", "설명", "문의처", "주최처", "지역 / 장소", "분위기 태그 1", "분위기 태그 2",
                "강도 태그", "목적 태그", "기간 태그", "규모 태그", "신뢰도", "분류 근거"
        };
    }

    private static DiscoverySheetStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return DiscoverySheetStatus.PENDING;
        }
        try {
            return DiscoverySheetStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return DiscoverySheetStatus.PENDING;
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String format(LocalDateTime value) {
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

    private static LocalDateTime firstDate(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
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
