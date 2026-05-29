package com.jjikmeok.app.domain.tag.dto.request;

import com.jjikmeok.app.domain.tag.entity.TagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "태그명은 필수입니다.")
        @Size(max = 50, message = "태그명은 50자 이하여야 합니다.")
        String name,
        @NotNull(message = "태그 타입은 필수입니다.")
        TagType type
) {
}
