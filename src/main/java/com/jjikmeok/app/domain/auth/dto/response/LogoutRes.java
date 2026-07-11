package com.jjikmeok.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그아웃 결과")
public record LogoutRes(

        @Schema(description = "로그아웃 완료 여부", example = "true")
        boolean loggedOut
) {
}
