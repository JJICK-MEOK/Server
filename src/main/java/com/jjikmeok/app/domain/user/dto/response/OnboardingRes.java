package com.jjikmeok.app.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record OnboardingRes(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "온보딩 ID", example = "1")
        Long onboardingId,

        @Schema(description = "온보딩 완료 여부", example = "true")
        boolean completed,

        @Schema(description = "선택한 주제 태그 ID 목록")
        List<Long> topicTagIds,

        @Schema(description = "선택한 지역 ID 목록")
        List<Long> regionIds,

        @Schema(description = "선택한 취향 태그 ID 목록")
        List<Long> preferenceTagIds
) {
}
