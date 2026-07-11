package com.jjikmeok.app.domain.user.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;

import java.util.List;

public record UserProfileMeRes(
        String nickname,
        String profileImageUrl,
        List<TagResponse> tags
) {

    public record TagResponse(
            Long id,
            String name,
            TagType type,
            TagGroupType groupType
    ) {
    }
}
