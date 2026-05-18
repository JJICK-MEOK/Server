package com.jjikmeok.app.domain.user.controller;

import com.jjikmeok.app.domain.user.dto.request.OnboardingReq;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.service.OnboardingService;
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
@Tag(name = "User Onboarding", description = "사용자 온보딩 API")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "사용자 온보딩 완료", description = "로그인한 사용자의 관심 주제, 활동 지역, 취향 태그를 한 번에 저장하고 온보딩을 완료 처리한다.")
    @PostMapping
    public ApiResponse<OnboardingRes> completeOnboarding(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingReq request
    ) {
        OnboardingRes response = onboardingService.completeOnboarding(userId, request);
        return ApiResponse.success(response);
    }
}
