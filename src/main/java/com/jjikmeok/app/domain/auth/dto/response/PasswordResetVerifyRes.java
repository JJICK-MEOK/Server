package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 인증번호 검증 응답")
public record PasswordResetVerifyRes(

        @Schema(description = "비밀번호를 재설정할 계정 이메일", example = "user@example.com")
        String email,

        @Schema(description = "1회용 resetToken", example = "qJk2W9m0pYx...")
        String resetToken,

        @Schema(description = "resetToken 만료 시간(초)", example = "300")
        int expiresInSeconds
) {
}
