package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "activity_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_activity_images_activity_sort_order",
                        columnNames = {"activity_id", "sort_order"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean isThumbnail;

    public static ActivityImage create(Activity activity, String imageUrl, Integer sortOrder, Boolean isThumbnail) {
        ActivityImage activityImage = new ActivityImage();
        activityImage.activity = activity;
        activityImage.imageUrl = imageUrl;
        activityImage.sortOrder = sortOrder != null ? sortOrder : 0;
        activityImage.isThumbnail = isThumbnail != null ? isThumbnail : false;
        return activityImage;
    }

    public void update(String imageUrl, Integer sortOrder, Boolean isThumbnail) {
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        if (isThumbnail != null) {
            this.isThumbnail = isThumbnail;
        }
    }
}
