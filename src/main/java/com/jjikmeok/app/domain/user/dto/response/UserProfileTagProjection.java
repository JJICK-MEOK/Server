package com.jjikmeok.app.domain.user.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;

public interface UserProfileTagProjection {

    Long getId();

    String getName();

    TagType getType();

    TagGroupType getTagGroupType();
}
