package com.jjikmeok.app.domain.tag.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "태그 상세 조회 응답")
public record TagDetailRes(
        @Schema(description = "태그 ID", example = "1")
        Long id,

        @Schema(description = "태그명", example = "차분")
        String name,

        @Schema(description = "태그 타입", example = "PREFERENCE_TAG")
        TagType type,

        @Schema(description = "태그 그룹 타입", example = "MOOD", nullable = true)
        TagGroupType tagGroupType
) {
}
