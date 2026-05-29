package com.jjikmeok.app.domain.auth.dto.response;

import com.jjikmeok.app.domain.user.entity.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginRes(

        @Schema(description = "발급된 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "발급된 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간(초)", example = "3600")
        int expiresIn,

        @Schema(description = "사용자 등록 상태", example = "PROFILE_COMPLETED")
        RegistrationStatus registrationStatus
) {
}
