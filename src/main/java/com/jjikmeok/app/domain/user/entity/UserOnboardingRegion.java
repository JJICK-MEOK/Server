package com.jjikmeok.app.domain.user.entity;

import com.jjikmeok.app.domain.region.entity.Region;
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
        name = "user_onboarding_regions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_onboarding_regions_onboarding_region",
                        columnNames = {"user_onboarding_id", "region_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOnboardingRegion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_onboarding_id", nullable = false)
    private UserOnboarding userOnboarding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    public static UserOnboardingRegion create(UserOnboarding userOnboarding, Region region) {
        UserOnboardingRegion userOnboardingRegion = new UserOnboardingRegion();
        userOnboardingRegion.userOnboarding = userOnboarding;
        userOnboardingRegion.region = region;
        return userOnboardingRegion;
    }
}
