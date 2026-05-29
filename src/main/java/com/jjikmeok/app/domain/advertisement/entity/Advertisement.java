package com.jjikmeok.app.domain.advertisement.entity;

import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "advertisements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Advertisement extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "redirect_url", nullable = false, length = 500)
    private String redirectUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdvertisementPosition position;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "click_count", nullable = false)
    private Integer clickCount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    public Advertisement(
            String title,
            String imageUrl,
            String redirectUrl,
            AdvertisementPosition position,
            Integer sortOrder,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isActive
    ) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.redirectUrl = redirectUrl;
        this.position = position;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.viewCount = 0;
        this.clickCount = 0;
        this.isActive = isActive != null ? isActive : true;
    }

    public void update(
            String title,
            String imageUrl,
            String redirectUrl,
            AdvertisementPosition position,
            Integer sortOrder,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean isActive
    ) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.redirectUrl = redirectUrl;
        this.position = position;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.startAt = startAt;
        this.endAt = endAt;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void deactivate() {
        this.isActive = false;
    }
}
