package com.jjikmeok.app.domain.activity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityImageRequest(
        @NotBlank(message = "활동 이미지 URL은 필수입니다.")
        @Size(max = 500, message = "활동 이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,
        @Min(value = 0, message = "이미지 노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder,
        Boolean isThumbnail
) {
}
