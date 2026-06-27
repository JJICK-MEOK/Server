package com.jjikmeok.app.domain.page.converter;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityTag;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.PreferenceTagGroup;
import com.jjikmeok.app.domain.image.entity.Image;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ImageItemResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class PageConverter {

    private static final int CARD_TAG_LIMIT = 2;
    private static final int DETAIL_TAG_LIMIT = 3;

    private PageConverter() {
    }

    public static ActivityCardResponse toCard(Activity activity, boolean liked, boolean isAd, LocalDate today) {
        Deadline deadline = deadline(activity.getRecruitEndAt(), today);

        return new ActivityCardResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getThumbnailUrl(),
                deadline.daysUntilRecruitEnd(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getAddress(),
                activity.getActivityType(),
                activity.getCategory(),
                randomHashtags(activity, CARD_TAG_LIMIT),
                isAd,
                activity.getPrice(),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                liked,
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getRecruitStartAt(),
                activity.getRecruitEndAt()
        );
    }

    public static ActivityDetailPageResponse toDetail(
            Activity activity,
            List<Image> images,
            boolean liked,
            LocalDate today
    ) {
        Deadline deadline = deadline(activity.getRecruitEndAt(), today);

        return new ActivityDetailPageResponse(
                activity.getId(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getThumbnailUrl(),
                imageItems(activity, images),
                activity.getSourceUrl(),
                activity.getAddress(),
                activity.getOrganizer(),
                activity.getContactInfo(),
                activity.getTarget(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getRecruitStartAt(),
                activity.getRecruitEndAt(),
                deadline.daysUntilRecruitEnd(),
                activity.getPrice(),
                activity.getActivityType(),
                activity.getCategory(),
                randomHashtags(activity, DETAIL_TAG_LIMIT),
                activity.getSourceType(),
                activity.getExternalId(),
                activity.getApprovalStatus(),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                liked,
                activity.getIsActive(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

    private static List<String> randomHashtags(Activity activity, int limit) {
        List<String> candidates = new ArrayList<>(tagCandidates(activity));
        Collections.shuffle(candidates);
        return candidates.stream()
                .limit(limit)
                .toList();
    }

    private static List<String> tagCandidates(Activity activity) {
        Map<PreferenceTagGroup, List<String>> tagsByGroup = new EnumMap<>(PreferenceTagGroup.class);
        for (ActivityTag activityTag : activity.getTags()) {
            if (activityTag.getTag() == null) {
                continue;
            }

            PreferenceTag preferenceTag = preferenceTag(activityTag.getTag().getName());
            if (preferenceTag == null) {
                continue;
            }

            tagsByGroup.computeIfAbsent(preferenceTag.getGroup(), ignored -> new ArrayList<>())
                    .add(preferenceTag.getHashtag());
        }

        List<String> candidates = new ArrayList<>();
        addOne(candidates, tagsByGroup, PreferenceTagGroup.MOOD);
        addOne(candidates, tagsByGroup, PreferenceTagGroup.INTENSITY);
        addOne(candidates, tagsByGroup, PreferenceTagGroup.PURPOSE);
        addOne(candidates, tagsByGroup, PreferenceTagGroup.DURATION);
        addOne(candidates, tagsByGroup, PreferenceTagGroup.SIZE);
        return candidates;
    }

    private static void addOne(List<String> candidates, Map<PreferenceTagGroup, List<String>> tagsByGroup, PreferenceTagGroup group) {
        List<String> values = tagsByGroup.getOrDefault(group, List.of()).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (values.isEmpty()) {
            return;
        }

        List<String> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled);
        candidates.add(shuffled.getFirst());
    }

    private static PreferenceTag preferenceTag(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.startsWith("#") ? label.substring(1) : label;
        for (PreferenceTag preferenceTag : PreferenceTag.values()) {
            if (preferenceTag.getLabel().equals(normalized)) {
                return preferenceTag;
            }
        }
        return null;
    }

    private static List<ImageItemResponse> imageItems(Activity activity, List<Image> images) {
        List<ImageItemResponse> items = new ArrayList<>();

        for (Image image : images) {
            items.add(new ImageItemResponse(
                    image.getId(),
                    image.getImageUrl(),
                    image.getSortOrder(),
                    image.getIsThumbnail()
            ));
        }

        if (items.isEmpty() && activity.getThumbnailUrl() != null && !activity.getThumbnailUrl().isBlank()) {
            items.add(new ImageItemResponse(null, activity.getThumbnailUrl(), 0, true));
        }

        return items;
    }


    private static Deadline deadline(LocalDateTime recruitEndAt, LocalDate today) {
        if (recruitEndAt == null) {
            return new Deadline(null);
        }
        if (recruitEndAt.getYear() >= 2999) {
            return new Deadline(null);
        }

        long days = ChronoUnit.DAYS.between(today, recruitEndAt.toLocalDate());
        return new Deadline(Math.toIntExact(days));
    }

    private record Deadline(Integer daysUntilRecruitEnd) {
    }
}
