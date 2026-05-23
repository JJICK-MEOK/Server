package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.region.entity.Region;

public class ActivityConverter {

    public static Activity toEntity(ActivityRequest request, Region region) {
        return Activity.builder()
                .region(region)
                .title(request.title())
                .thumbnailUrl(request.thumbnailUrl())
                .uri(request.uri())
                .location(request.location())
                .recruitStartAt(request.recruitStartAt())
                .recruitEndAt(request.recruitEndAt())
                .activityStartAt(request.activityStartAt())
                .activityEndAt(request.activityEndAt())
                .ageRange(request.ageRange())
                .price(request.price())
                .description(request.description())
                .isActive(request.isActive())
                .build();
    }

    public static ActivityDetailResponse toDetailResponse(Activity activity) {
        return new ActivityDetailResponse(
                activity.getId(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getTitle(),
                activity.getThumbnailUrl(),
                activity.getUri(),
                activity.getLocation(),
                activity.getRecruitStartAt(),
                activity.getRecruitEndAt(),
                activity.getActivityStartAt(),
                activity.getActivityEndAt(),
                activity.getAgeRange(),
                activity.getPrice(),
                activity.getDescription(),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                activity.getIsActive(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }

    public static ActivitySummaryResponse toSummaryResponse(Activity activity) {
        return new ActivitySummaryResponse(
                activity.getId(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getTitle(),
                activity.getThumbnailUrl(),
                activity.getLocation(),
                activity.getRecruitEndAt(),
                activity.getActivityStartAt(),
                activity.getActivityEndAt(),
                activity.getAgeRange(),
                activity.getPrice(),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                activity.getCreatedAt()
        );
    }
}
