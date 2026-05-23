package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증번호 검증 응답")
public record EmailVerificationVerifyRes(

        @Schema(
                description = "인증번호를 검증한 이메일",
                example = "example@email.com"
        )
        String email,

        @Schema(
                description = "인증 성공 여부",
                example = "true"
        )
        boolean verified
) {
}
