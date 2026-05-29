package com.jjikmeok.app.domain.tag.converter;

import com.jjikmeok.app.domain.tag.dto.response.TagResponse;
import com.jjikmeok.app.domain.tag.entity.Tag;

public class TagConverter {

    private TagConverter() {
    }

    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getType());
    }
}
