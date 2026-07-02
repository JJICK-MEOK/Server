package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 인증번호 검증 요청")
public record PasswordResetVerifyReq(

        @Schema(description = "비밀번호를 재설정할 계정 이메일", example = "user@example.com")
        @NotBlank
        @Email
        String email,

        @Schema(description = "이메일로 발송된 6자리 인증번호", example = "123456")
        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String code
) {
}
