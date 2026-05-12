package com.jjikmeok.app.domain.test.controller;

import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Operation(summary = "Swagger 응답 테스트", description = "공통 응답 포맷 확인용 공개 API입니다.")
    @GetMapping("/response")
    public ApiResponse<List<String>> testResponse() {
        List<String> teamMembers = List.of("팀원1", "팀원2", "팀원3", "팀원4");
        return ApiResponse.success("API 공통 응답 테스트 성공!", teamMembers);
    }

    @Operation(
            summary = "JWT 인증 확인",
            description = "Swagger Authorize에 Bearer 토큰을 입력한 뒤 호출하면 현재 인증된 사용자 정보를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/auth-check")
    public ApiResponse<Map<String, Object>> authCheck(Authentication authentication) {
        Map<String, Object> result = Map.of(
                "authenticated", authentication != null && authentication.isAuthenticated(),
                "principal", authentication != null ? authentication.getPrincipal() : "anonymous",
                "authorities", authentication != null ? authentication.getAuthorities() : List.of()
        );
        return ApiResponse.success("JWT 인증이 확인되었습니다.", result);
    }
}
