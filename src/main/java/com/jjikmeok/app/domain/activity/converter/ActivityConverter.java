package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityTag;
import com.jjikmeok.app.domain.region.entity.Region;

import java.util.List;

public class ActivityConverter {

    public static Activity toEntity(ActivityRequest request, Region region) {
        return Activity.builder()
                .region(region)
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .sourceUrl(request.sourceUrl())
                .address(request.address())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .recruitStartAt(request.recruitStartAt())
                .recruitEndAt(request.recruitEndAt())
                .price(request.price())
                .activityType(request.activityType())
                .category(request.category())
                .sourceType(request.sourceType())
                .externalId(request.externalId())
                .approvalStatus(request.approvalStatus())
                .isActive(request.isActive())
                .build();
    }

    public static ActivityDetailResponse toDetailResponse(Activity activity) {
        return new ActivityDetailResponse(
                activity.getId(),
                activity.getRegion().getId(),
                activity.getRegion().getName(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getThumbnailUrl(),
                activity.getSourceUrl(),
                activity.getAddress(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getRecruitStartAt(),
                activity.getRecruitEndAt(),
                activity.getPrice(),
                activity.getActivityType(),
                activity.getCategory(),
                tagNames(activity),
                activity.getSourceType(),
                activity.getExternalId(),
                activity.getApprovalStatus(),
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
                activity.getAddress(),
                activity.getStartAt(),
                activity.getEndAt(),
                activity.getRecruitEndAt(),
                activity.getActivityType(),
                activity.getCategory(),
                tagNames(activity),
                activity.getPrice(),
                activity.getSourceType(),
                activity.getApprovalStatus(),
                activity.getViewCount(),
                activity.getLikeCount(),
                activity.getReviewCount(),
                activity.getCreatedAt()
        );
    }

    private static List<String> tagNames(Activity activity) {
        return activity.getTags().stream()
                .map(ActivityTag::getTag)
                .map(tag -> tag.getName())
                .toList();
    }
}
