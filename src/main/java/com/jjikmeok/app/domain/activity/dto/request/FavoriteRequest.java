package com.jjikmeok.app.domain.activity.dto.request;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull(message = "활동 ID는 필수입니다.")
        Long activityId
) {
}
