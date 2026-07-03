package com.jjikmeok.app.domain.page.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ActivityFavoritePageResponse(
        @Schema(description = "사용자가 찜한 활동 카드 목록")
        List<ActivityCardResponse> activities
) {
}
