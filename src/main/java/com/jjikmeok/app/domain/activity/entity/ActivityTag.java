package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.global.common.BaseEntity;
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
        name = "activity_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_activity_tags_activity_tag",
                        columnNames = {"activity_id", "tag_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static ActivityTag create(Activity activity, Tag tag) {
        ActivityTag activityTag = new ActivityTag();
        activityTag.activity = activity;
        activityTag.tag = tag;
        return activityTag;
    }
}
