package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.domain.activity.enums.AgeRange;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(length = 255)
    private String location;

    @Column(name = "recruit_start_at")
    private LocalDateTime recruitStartAt;

    @Column(name = "recruit_end_at", nullable = false)
    private LocalDateTime recruitEndAt;

    @Column(name = "activity_start_at")
    private LocalDateTime activityStartAt;

    @Column(name = "activity_end_at")
    private LocalDateTime activityEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 50)
    private AgeRange ageRange;

    @Column(nullable = false)
    private Integer price;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public Activity(Region region, String title, String thumbnailUrl, String uri, String location,
                    LocalDateTime recruitStartAt, LocalDateTime recruitEndAt, LocalDateTime activityStartAt,
                    LocalDateTime activityEndAt, AgeRange ageRange, Integer price, String description, Boolean isActive) {
        this.region = region;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.uri = uri;
        this.location = location;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.activityStartAt = activityStartAt;
        this.activityEndAt = activityEndAt;
        this.ageRange = ageRange;
        this.price = price != null ? price : 0;
        this.description = description;
        this.viewCount = 0;
        this.likeCount = 0;
        this.reviewCount = 0;
        this.isActive = isActive != null ? isActive : true;
    }

    public void update(Region region, String title, String thumbnailUrl, String uri, String location,
                       LocalDateTime recruitStartAt, LocalDateTime recruitEndAt, LocalDateTime activityStartAt,
                       LocalDateTime activityEndAt, AgeRange ageRange, Integer price, String description, Boolean isActive) {
        this.region = region;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.uri = uri;
        this.location = location;
        this.recruitStartAt = recruitStartAt;
        this.recruitEndAt = recruitEndAt;
        this.activityStartAt = activityStartAt;
        this.activityEndAt = activityEndAt;
        this.ageRange = ageRange;
        this.price = price != null ? price : 0;
        this.description = description;
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
