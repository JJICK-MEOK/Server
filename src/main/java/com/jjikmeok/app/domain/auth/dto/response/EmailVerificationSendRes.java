package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증번호 전송 응답")
public record EmailVerificationSendRes(

        @Schema(
                description = "인증번호가 발송된 이메일",
                example = "example@email.com"
        )
        String email,
        @Schema(
                description = "인증번호 유효 시간, 단위는 초",
                example = "180"
        )
        int expiresIn
) {
}