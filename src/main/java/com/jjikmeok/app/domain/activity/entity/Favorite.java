package com.jjikmeok.app.domain.activity.entity;

import com.jjikmeok.app.domain.user.entity.User;
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
@Table(name = "activity_favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_activity_favorites_user_activity", columnNames = {"user_id", "activity_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    public static Favorite create(User user, Activity activity) {
        Favorite favorite = new Favorite();
        favorite.user = user;
        favorite.activity = activity;
        return favorite;
    }
}
