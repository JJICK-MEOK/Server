package com.jjikmeok.app.domain.tag.dto.response;

import com.jjikmeok.app.domain.activity.enums.PreferenceTagGroup;

import java.util.List;

public record PreferenceTagGroupResponse(
        PreferenceTagGroup group,
        String label,
        String description,
        List<TagResponse> tags
) {
}
