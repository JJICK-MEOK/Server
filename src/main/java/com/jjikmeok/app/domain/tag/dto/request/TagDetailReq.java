package com.jjikmeok.app.domain.tag.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "태그 상세 조회 요청")
public record TagDetailReq(
        @Schema(description = "태그 ID", example = "1")
        @NotNull(message = "태그 ID는 필수입니다.")
        @Positive(message = "태그 ID는 양수여야 합니다.")
        Long id
) {
}
