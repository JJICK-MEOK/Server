package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 인증번호 발송 응답")
public record PasswordResetSendRes(

        @Schema(description = "정규화된 이메일", example = "user@example.com")
        String email,

        @Schema(description = "인증번호 만료 시간(초)", example = "600")
        int expiresIn
) {
}
