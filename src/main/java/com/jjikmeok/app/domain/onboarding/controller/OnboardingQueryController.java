package com.jjikmeok.app.domain.onboarding.controller;

import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingPreferenceTagRes;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.onboarding.service.query.OnboardingQueryService;
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
@Tag(name = "사용자 온보딩", description = "사용자 온보딩 조회 API")
public class OnboardingQueryController {

    private final OnboardingQueryService onboardingQueryService;

    @Operation(
            summary = "사용자 온보딩 조회",
            description = "로그인한 사용자의 저장된 온보딩 선택 정보를 조회합니다."
    )
    @GetMapping
    public ApiResponse<OnboardingRes> getOnboarding(
            @AuthenticationPrincipal final Long userId
    ) {
        final OnboardingRes response = onboardingQueryService.getOnboarding(userId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "온보딩 취향 태그 수정 목록 조회",
            description = "온보딩 수정 화면에서 사용할 전체 취향 태그와 로그인 사용자의 선택 여부를 조회합니다."
    )
    @GetMapping("/preference-tags")
    public ApiResponse<List<OnboardingPreferenceTagRes>> getPreferenceTagsForEdit(
            @AuthenticationPrincipal final Long userId
    ) {
        final List<OnboardingPreferenceTagRes> response = onboardingQueryService.getPreferenceTagsForEdit(userId);
        return ApiResponse.success(response);
    }
}
