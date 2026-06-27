package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "handoff token 교환 요청")
public record HandoffTokenReq(

        @Schema(description = "1회용 handoff token", example = "A1b2C3d4...")
        @NotBlank(message = "handoff token은 필수입니다.")
        String handoffToken
) {
}
