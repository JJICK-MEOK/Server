package com.jjikmeok.app.domain.user.converter;

import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import com.jjikmeok.app.domain.user.entity.UserOnboardingRegion;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;

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
                true,
                topicTagIds,
                regionIds,
                preferenceTagIds
        );
    }
}
