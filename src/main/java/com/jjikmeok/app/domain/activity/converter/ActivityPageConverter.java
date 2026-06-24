package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityTag;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityImageItemResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ActivityPageConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private ActivityPageConverter() {
    }

    public static ActivityCardResponse toCard(Activity activity, boolean liked, LocalDate today) {
        Deadline deadline = deadline(activity.getRecruitEndAt(), today);

        return new ActivityCardResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getThumbnailUrl(),
                deadline.dDay(),
                deadline.daysUntilRecruitEnd(),
                deadline.text(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getAddress(),
                activity.getActivityType(),
                activity.getActivityType().getLabel(),
                activity.getCategory(),
                activity.getCategory().getLabel(),
                hashtags(activity),
                activity.getPrice(),
                priceLabel(activity.getPrice()),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                liked,
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getRecruitEndAt()
        );
    }

    public static ActivityDetailPageResponse toDetail(
            Activity activity,
            List<ActivityImage> images,
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
                periodText(activity.getStartAt(), activity.getEndAt()),
                periodText(activity.getRecruitStartAt(), activity.getRecruitEndAt()),
                deadline.dDay(),
                deadline.daysUntilRecruitEnd(),
                deadline.text(),
                activity.getPrice(),
                priceLabel(activity.getPrice()),
                activity.getActivityType(),
                activity.getActivityType().getLabel(),
                activity.getCategory(),
                activity.getCategory().getLabel(),
                hashtags(activity),
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

    public static String priceLabel(Integer price) {
        if (price == null) {
            return "금액 확인";
        }
        if (price == 0) {
            return "무료";
        }
        return String.format("%,d원", price);
    }

    public static List<String> hashtags(Activity activity) {
        Set<String> values = new LinkedHashSet<>();

        activity.getTags().stream()
                .map(ActivityTag::getTag)
                .map(tag -> hashtag(tag.getName()))
                .forEach(values::add);

        if (values.isEmpty()) {
            values.add(hashtag(activity.getCategory().getLabel()));
            values.add(hashtag(activity.getActivityType().getLabel()));
            values.add(hashtag(priceLabel(activity.getPrice())));
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(4)
                .toList();
    }

    private static List<ActivityImageItemResponse> imageItems(Activity activity, List<ActivityImage> images) {
        List<ActivityImageItemResponse> items = new ArrayList<>();

        for (ActivityImage image : images) {
            items.add(new ActivityImageItemResponse(
                    image.getId(),
                    image.getImageUrl(),
                    image.getSortOrder(),
                    image.getIsThumbnail()
            ));
        }

        if (items.isEmpty() && activity.getThumbnailUrl() != null && !activity.getThumbnailUrl().isBlank()) {
            items.add(new ActivityImageItemResponse(null, activity.getThumbnailUrl(), 0, true));
        }

        return items;
    }

    private static String periodText(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null && endAt == null) {
            return "원문에서 확인";
        }
        if (startAt == null) {
            return "~ " + date(endAt);
        }
        if (endAt == null) {
            return date(startAt) + " ~";
        }
        if (startAt.toLocalDate().equals(endAt.toLocalDate())) {
            return date(startAt);
        }
        return date(startAt) + " ~ " + date(endAt);
    }

    private static String date(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_FORMATTER);
    }

    private static String hashtag(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.startsWith("#") ? value : "#" + value;
    }

    private static Deadline deadline(LocalDateTime recruitEndAt, LocalDate today) {
        if (recruitEndAt == null) {
            return new Deadline("상시", null, "상시 모집");
        }

        long days = ChronoUnit.DAYS.between(today, recruitEndAt.toLocalDate());
        if (days < 0) {
            return new Deadline("마감", days, "모집 마감");
        }
        if (days == 0) {
            return new Deadline("D-DAY", 0L, "오늘 마감");
        }
        return new Deadline("D-" + days, days, days + "일 남음");
    }

    private record Deadline(String dDay, Long daysUntilRecruitEnd, String text) {
    }
}
