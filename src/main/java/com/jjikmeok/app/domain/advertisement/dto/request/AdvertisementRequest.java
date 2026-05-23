package com.jjikmeok.app.domain.advertisement.dto.request;

import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdvertisementRequest(
        @NotBlank(message = "광고 제목은 필수입니다.")
        @Size(max = 100, message = "광고 제목은 100자 이하여야 합니다.")
        String title,
        @NotBlank(message = "광고 이미지 URL은 필수입니다.")
        @Size(max = 500, message = "광고 이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,
        @NotBlank(message = "광고 이동 URL은 필수입니다.")
        @Size(max = 500, message = "광고 이동 URL은 500자 이하여야 합니다.")
        String redirectUrl,
        @NotNull(message = "광고 노출 위치는 필수입니다.")
        AdvertisementPosition position,
        @Min(value = 0, message = "광고 노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isActive
) {
}
