package com.jjikmeok.app.domain.user.entity;

import com.jjikmeok.app.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "user_onboardings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_onboardings_user_id", columnNames = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboarding extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static UserOnboarding create(User user) {
        UserOnboarding userOnboarding = new UserOnboarding();
        userOnboarding.user = user;
        return userOnboarding;
    }
}
