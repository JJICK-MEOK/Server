package com.jjikmeok.app.domain.onboarding.converter;

import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingPreferenceTagRes;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.entity.RegistrationStatus;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboarding;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingRegion;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;

import java.util.List;

public final class OnboardingConverter {

    private OnboardingConverter() {
    }

    public static UserOnboarding toUserOnboarding(User user) {
        return UserOnboarding.create(user);
    }

    public static UserOnboardingRegion toUserOnboardingRegion(UserOnboarding userOnboarding, Region region) {
        return UserOnboardingRegion.create(userOnboarding, region);
    }

    public static UserOnboardingTag toUserOnboardingTag(UserOnboarding userOnboarding, Tag tag) {
        return UserOnboardingTag.create(userOnboarding, tag);
    }

    public static OnboardingRes toOnboardingResponse(
            User user,
            UserOnboarding userOnboarding,
            List<Long> topicTagIds,
            List<Long> regionIds,
            List<Long> preferenceTagIds
    ) {
        return new OnboardingRes(
                user.getId(),
                userOnboarding.getId(),
                user.getRegistrationStatus() == RegistrationStatus.ONBOARDING_COMPLETED,
                topicTagIds,
                regionIds,
                preferenceTagIds
        );
    }

    public static OnboardingPreferenceTagRes toOnboardingPreferenceTagRes(Tag tag, boolean selected) {
        return new OnboardingPreferenceTagRes(
                tag.getId(),
                tag.getName(),
                tag.getType(),
                selected
        );
    }
}
