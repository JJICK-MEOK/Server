package com.jjikmeok.app.domain.onboarding.dto.response;

import com.jjikmeok.app.domain.tag.entity.TagType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingPreferenceTagRes(
        @Schema(description = "취향 태그 ID", example = "20")
        Long id,

        @Schema(description = "취향 태그 이름", example = "#편안한")
        String name,

        @Schema(description = "태그 타입", example = "PREFERENCE_TAG")
        TagType type,

        @Schema(description = "현재 사용자의 선택 여부", example = "true")
        boolean selected
) {
}
