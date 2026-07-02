package com.jjikmeok.app.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetReq(

        @Schema(description = "인증번호 검증 후 발급된 1회용 resetToken", example = "qJk2W9m0pYx...")
        @NotBlank
        String resetToken,

        @Schema(description = "새 비밀번호", example = "NewPassword123!")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`])[a-zA-Z\\d!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`]+$"
        )
        String newPassword,

        @Schema(description = "새 비밀번호 확인", example = "NewPassword123!")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`])[a-zA-Z\\d!@#$%^&*()_+=\\-\\[\\]{}|;:,.<>?/~`]+$"
        )
        String newPasswordConfirm
) {
}
