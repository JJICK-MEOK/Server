package com.jjikmeok.app.domain.user.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagType;

public record OnboardingPreferenceTagResponse(
        Long id,
        String name,
        TagType type,
        boolean selected
) {
}
