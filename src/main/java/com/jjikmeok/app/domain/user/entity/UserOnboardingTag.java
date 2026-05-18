package com.jjikmeok.app.domain.user.entity;

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
        name = "user_onboarding_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_onboarding_tags_onboarding_tag",
                        columnNames = {"user_onboarding_id", "tag_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_onboarding_id", nullable = false)
    private UserOnboarding userOnboarding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static UserOnboardingTag create(UserOnboarding userOnboarding, Tag tag) {
        UserOnboardingTag userOnboardingTag = new UserOnboardingTag();
        userOnboardingTag.userOnboarding = userOnboarding;
        userOnboardingTag.tag = tag;
        return userOnboardingTag;
    }
}
