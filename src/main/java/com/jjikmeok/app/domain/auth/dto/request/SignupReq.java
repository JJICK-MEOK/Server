package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupReq(

        @Schema(description = "회원가입할 이메일", example = "user@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "회원가입 비밀번호", example = "Password123!")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`])[a-zA-Z\\d!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`]+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다."
        )
        String password
) {
}
