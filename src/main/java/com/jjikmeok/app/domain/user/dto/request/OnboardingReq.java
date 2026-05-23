package com.jjikmeok.app.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnboardingReq(

        @Schema(description = "관심 주제 태그 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "topicTagIds는 비어 있을 수 없습니다.")
        List<@NotNull(message = "topicTagIds에는 null이 포함될 수 없습니다.") Long> topicTagIds,

        @Schema(description = "활동 지역 ID 목록", example = "[10, 11]")
        @NotEmpty(message = "regionIds는 비어 있을 수 없습니다.")
        List<@NotNull(message = "regionIds에는 null이 포함될 수 없습니다.") Long> regionIds,

        @Schema(description = "취향 태그 ID 목록", example = "[20, 21, 22]")
        @NotEmpty(message = "preferenceTagIds는 비어 있을 수 없습니다.")
        List<@NotNull(message = "preferenceTagIds에는 null이 포함될 수 없습니다.") Long> preferenceTagIds
) {
}
