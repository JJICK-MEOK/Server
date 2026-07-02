package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 결과")
public record PasswordResetRes(

        @Schema(description = "비밀번호 재설정 완료 여부", example = "true")
        boolean reset
) {
}
