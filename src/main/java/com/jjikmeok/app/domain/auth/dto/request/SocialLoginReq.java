package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginReq(

        @Schema(description = "소셜 로그인 인가 코드", example = "4/0AQSTgQ...")
        @NotBlank(message = "인가 코드는 필수입니다.")
        String code
) {
}
