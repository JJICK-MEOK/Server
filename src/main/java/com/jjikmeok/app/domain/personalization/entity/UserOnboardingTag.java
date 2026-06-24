package com.jjikmeok.app.domain.personalization.entity;

import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_onboarding_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_onboarding_tags_onboarding_tag",
                        columnNames = {"user_onboarding_id", "tag_id"}
                )
        }
)
public class UserOnboardingTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * user_onboarding_tags.user_onboarding_id
     * -> user_onboarding.id
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_onboarding_id", nullable = false)
    private UserOnboarding userOnboarding;

    /*
     * user_onboarding_tags.tag_id
     * -> tags.id
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UserOnboardingTag(UserOnboarding userOnboarding, Tag tag) {
        this.userOnboarding = userOnboarding;
        this.tag = tag;
    }

    public static UserOnboardingTag of(UserOnboarding userOnboarding, Tag tag) {
        return new UserOnboardingTag(userOnboarding, tag);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
