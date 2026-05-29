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
    // 🌟 1순위 데이터 병합 및 2순위 누락 데이터 선택적 복원 메서드
    public NormalizedActivity copyWithFallbackFields(
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
                this.title,
                isMissing(this.description) && aiDescription != null ? aiDescription : this.description,
                this.thumbnailUrl,
                this.sourceUrl,
                this.address,
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
        return value == null || value.isBlank() || value.contains("원문 링크") || value.contains("확인하세요");
    }
}