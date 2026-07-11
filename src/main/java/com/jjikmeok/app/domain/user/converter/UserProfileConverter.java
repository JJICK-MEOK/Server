package com.jjikmeok.app.domain.user.converter;

import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileTagProjection;
import com.jjikmeok.app.domain.user.entity.UserProfile;

import java.util.List;

public final class UserProfileConverter {

    private UserProfileConverter() {
    }

    public static UserProfileMeRes toMyProfileResponse(
            UserProfile userProfile,
            List<UserProfileTagProjection> tagRows
    ) {
        return new UserProfileMeRes(
                userProfile.getNickname(),
                userProfile.getProfileImageUrl(),
                tagRows.stream()
                        .map(UserProfileConverter::toTagResponse)
                        .toList()
        );
    }

    public static UserProfileMeRes.TagResponse toTagResponse(UserProfileTagProjection row) {
        return new UserProfileMeRes.TagResponse(
                row.getId(),
                row.getName(),
                row.getType(),
                row.getTagGroupType()
        );
    }
}
