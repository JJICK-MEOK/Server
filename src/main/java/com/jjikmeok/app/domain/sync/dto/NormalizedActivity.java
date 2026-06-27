package com.jjikmeok.app.domain.sync.dto;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;

import java.time.LocalDateTime;

public record NormalizedActivity(
        String title,
        String description,
        String thumbnailUrl,
        String sourceUrl,
        String address,
        String organizer,
        String contactInfo,
        String target,
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
        Boolean active
) {
    public NormalizedActivity copyWithFallbackFields(
            String aiTitle,
            String aiAddress,
            LocalDateTime aiRecruitStartAt,
            LocalDateTime aiRecruitEndAt,
            LocalDateTime aiStartAt,
            LocalDateTime aiEndAt,
            Integer aiPrice,
            String aiDescription,
            String aiTarget,
            String aiContactInfo,
            String aiOrganizer
    ) {
        return new NormalizedActivity(
                isMissing(this.title) && aiTitle != null ? aiTitle : this.title,
                isMissing(this.description) && aiDescription != null ? aiDescription : this.description,
                this.thumbnailUrl,
                this.sourceUrl,
                isMissing(this.address) && aiAddress != null ? aiAddress : this.address,
                isMissing(this.organizer) && aiOrganizer != null ? aiOrganizer : this.organizer,
                isMissing(this.contactInfo) && aiContactInfo != null ? aiContactInfo : this.contactInfo,
                isMissing(this.target) && aiTarget != null ? aiTarget : this.target,
                this.startAt == null ? aiStartAt : this.startAt,
                this.endAt == null ? aiEndAt : this.endAt,
                this.recruitStartAt == null ? aiRecruitStartAt : this.recruitStartAt,
                (this.recruitEndAt == null || this.recruitEndAt.getYear() >= 2099) ? aiRecruitEndAt : this.recruitEndAt,
                this.price == null ? aiPrice : this.price,
                this.activityType,
                this.category,
                this.sourceType,
                this.externalId,
                this.approvalStatus,
                this.active
        );
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank() || value.contains("?먮Ц 留곹겕") || value.contains("?뺤씤?섏꽭??");
    }
}
