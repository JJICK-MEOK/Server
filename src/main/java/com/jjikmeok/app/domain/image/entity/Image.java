package com.jjikmeok.app.domain.image.entity;

import com.jjikmeok.app.domain.activity.entity.Activity;
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
public class Image extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean isThumbnail;

    public static Image create(Activity activity, String imageUrl, Integer sortOrder, Boolean isThumbnail) {
        Image image = new Image();
        image.activity = activity;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder != null ? sortOrder : 0;
        image.isThumbnail = isThumbnail != null ? isThumbnail : false;
        return image;
    }

    public void update(String imageUrl, Integer sortOrder, Boolean isThumbnail) {
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        if (isThumbnail != null) {
            this.isThumbnail = isThumbnail;
        }
    }
}
