package com.jjikmeok.app.domain.onboarding.controller;

import com.jjikmeok.app.domain.onboarding.dto.request.OnboardingReq;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.onboarding.service.command.OnboardingCommandService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/onboarding")
@Tag(name = "사용자 온보딩", description = "사용자 온보딩 관련 API")
public class OnboardingController {

    private final OnboardingCommandService onboardingCommandService;

    @Operation(
            summary = "사용자 온보딩 완료",
            description = "로그인한 사용자의 관심 주제, 활동 지역, 취향 태그를 저장하고 온보딩 완료 상태로 변경합니다."
    )
    @PostMapping
    public ApiResponse<OnboardingRes> completeOnboarding(
            @AuthenticationPrincipal final Long userId,
            @Valid @RequestBody final OnboardingReq request
    ) {
        final OnboardingRes response = onboardingCommandService.completeOnboarding(userId, request);
        return ApiResponse.success(response);
    }
}
