package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignupRes(

        @Schema(description = "회원가입된 사용자 ID", example = "1")
        Long userId,

        @Schema(description = "회원가입된 이메일", example = "user@example.com")
        String email
) {
}
