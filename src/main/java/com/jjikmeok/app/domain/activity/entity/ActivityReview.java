package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "activity_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_activity_reviews_user_activity", columnNames = {"user_id", "activity_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false)
    private Integer rating;

    @Lob
    private String reason;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    public static ActivityReview create(User user, Activity activity, Integer rating, String reason) {
        ActivityReview review = new ActivityReview();
        review.user = user;
        review.activity = activity;
        review.rating = rating;
        review.reason = reason;
        review.likeCount = 0;
        return review;
    }

    public void update(Integer rating, String reason) {
        this.rating = rating;
        this.reason = reason;
    }
}
