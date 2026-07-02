package com.jjikmeok.app.domain.tag.dto.request;

import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "태그 수정 요청")
public record TagUpdateReq(
        @Schema(description = "태그명", example = "활기")
        @NotBlank(message = "태그명은 필수입니다.")
        @Size(max = 50, message = "태그명은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "태그 타입", example = "PREFERENCE_TAG")
        @NotNull(message = "태그 타입은 필수입니다.")
        TagType type,

        @Schema(description = "태그 그룹 타입. 그룹이 없는 태그면 생략 가능합니다.", example = "MOOD")
        TagGroupType tagGroupType
) {
}
