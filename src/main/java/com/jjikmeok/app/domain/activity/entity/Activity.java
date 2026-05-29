package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String organizer;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    @Column(columnDefinition = "TEXT")
    private String target;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "recruit_start_at")
    private LocalDateTime recruitStartAt;

    @Column(name = "recruit_end_at")
    private LocalDateTime recruitEndAt;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityCategory category;

    @OneToMany(mappedBy = "activity")
    private List<ActivityTag> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Column(name = "external_id", columnDefinition = "TEXT")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    private ApprovalStatus approvalStatus;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Activity(
            Region region,
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
            Boolean isActive
    ) {
        this.region = region;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.sourceUrl = sourceUrl;
        this.address = address;
        this.organizer = organizer;
        this.contactInfo = contactInfo;
        this.target = target;
        this.startAt = startAt;
        this.endAt = endAt;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.price = price != null ? price : 0;
        this.activityType = activityType != null ? activityType : ActivityType.EVENT;
        this.category = category != null ? category : ActivityCategory.ETC;
        this.sourceType = sourceType != null ? sourceType : SourceType.URL_MANUAL;
        this.externalId = externalId;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.PENDING;
        this.viewCount = 0;
        this.likeCount = 0;
        this.reviewCount = 0;
        this.isActive = isActive != null ? isActive : true;
    }

    public void update(
            Region region,
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
        this.region = region;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.sourceUrl = sourceUrl;
        this.address = address;
        this.startAt = startAt;
        this.endAt = endAt;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.price = price != null ? price : 0;
        this.activityType = activityType != null ? activityType : ActivityType.EVENT;
        this.category = category != null ? category : ActivityCategory.ETC;
        this.sourceType = sourceType != null ? sourceType : SourceType.URL_MANUAL;
        this.externalId = externalId;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.PENDING;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateExtra(String organizer, String contactInfo, String target) {
        this.organizer = organizer;
        this.contactInfo = contactInfo;
        this.target = target;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.isActive = endAt == null || !endAt.isBefore(LocalDateTime.now());
    }

    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.isActive = false;
    }

    public void increaseReviewCount() {
        this.reviewCount++;
    }

    public void decreaseReviewCount() {
        if (this.reviewCount > 0) {
            this.reviewCount--;
        }
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
