package com.jjikmeok.app.domain.user.controller;

import com.jjikmeok.app.domain.user.dto.response.OnboardingPreferenceTagResponse;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.service.OnboardingQueryService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/onboarding")
@Tag(name = "User Onboarding", description = "User onboarding API")
public class OnboardingQueryController {

    private final OnboardingQueryService onboardingQueryService;

    @Operation(summary = "사용자 온보딩 조회", description = "Returns the current user's saved onboarding selections.")
    @GetMapping
    public ApiResponse<OnboardingRes> getOnboarding(
            @AuthenticationPrincipal Long userId
    ) {
        OnboardingRes response = onboardingQueryService.getOnboarding(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "취향 태그 수정 조회", description = "Returns all preference tags with the current user's selected state.")
    @GetMapping("/preference-tags")
    public ApiResponse<List<OnboardingPreferenceTagResponse>> getPreferenceTagsForEdit(
            @AuthenticationPrincipal Long userId
    ) {
        List<OnboardingPreferenceTagResponse> response = onboardingQueryService.getPreferenceTagsForEdit(userId);
        return ApiResponse.success(response);
    }
}
