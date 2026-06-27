package com.jjikmeok.app.domain.activity.dto.request;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ActivityRequest(
        @NotNull(message = "지역 ID는 필수입니다.") Long regionId,
        @NotBlank(message = "제목은 필수입니다.") String title,
        @NotBlank(message = "상세 설명은 필수입니다.") String description,
        String thumbnailUrl,
        @NotBlank(message = "sourceUrl은 필수입니다.") String sourceUrl,
        String address,
        String organizer,
        String contactInfo,
        String target,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime recruitStartAt,
        @NotNull(message = "모집 마감일은 필수입니다.") LocalDateTime recruitEndAt,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.") Integer price,
        @NotNull(message = "활동 유형은 필수입니다.") ActivityType activityType,
        @NotNull(message = "카테고리는 필수입니다.") ActivityCategory category,
        @NotNull(message = "소스 타입은 필수입니다.") SourceType sourceType,
        String externalId,
        ApprovalStatus approvalStatus,
        Boolean isActive
) {
    public ActivityRequest(
            Long regionId,
            String title,
            String description,
            String thumbnailUrl,
            String sourceUrl,
            String address,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime recruitStartAt,
            LocalDateTime recruitEndAt,
            Integer price,
            ActivityType activityType,
            ActivityCategory category,
            SourceType sourceType,
            String externalId,
            ApprovalStatus approvalStatus,
            Boolean isActive
    ) {
        this(
                regionId,
                title,
                description,
                thumbnailUrl,
                sourceUrl,
                address,
                null,
                null,
                null,
                startAt,
                endAt,
                recruitStartAt,
                recruitEndAt,
                price,
                activityType,
                category,
                sourceType,
                externalId,
                approvalStatus,
                isActive
        );
    }
}
