package com.jjikmeok.app.domain.user.dto.response;

import com.jjikmeok.app.domain.user.entity.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileCreateRes(

        @Schema(description = "회원가입 진행 상태", example = "PROFILE_COMPLETED")
        RegistrationStatus registrationStatus
) {
}
