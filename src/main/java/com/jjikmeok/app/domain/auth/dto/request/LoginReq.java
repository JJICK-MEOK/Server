package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginReq(

        @Schema(description = "로그인할 이메일", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "로그인 비밀번호", example = "Password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64)
        String password
) {
}
