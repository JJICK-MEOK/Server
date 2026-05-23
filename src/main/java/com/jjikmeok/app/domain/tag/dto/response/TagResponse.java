package com.jjikmeok.app.domain.tag.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagType;

public record TagResponse(
        Long id,
        String name,
        TagType type
) {
}
