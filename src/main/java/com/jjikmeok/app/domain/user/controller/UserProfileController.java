package com.jjikmeok.app.domain.user.controller;

import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;
import com.jjikmeok.app.domain.user.service.UserProfileService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/profile")
@Tag(name = "User Profile", description = "사용자 프로필 API")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "마이찍먹 사용자 프로필 조회", description = "로그인 사용자의 프로필과 온보딩 태그를 조회합니다.")
    @GetMapping
    public ApiResponse<UserProfileMeRes> getMyProfile(
            @AuthenticationPrincipal Long userId
    ) {
        UserProfileMeRes response = userProfileService.getMyProfile(userId);
        return ApiResponse.success("마이찍먹 사용자 프로필 조회 성공", response);
    }

    @Operation(summary = "사용자 프로필 생성", description = "로그인한 사용자의 프로필 정보를 최초 생성합니다.")
    @PostMapping
    public ApiResponse<UserProfileCreateRes> createProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileCreateReq request
    ) {
        UserProfileCreateRes response = userProfileService.createProfile(userId, request);
        return ApiResponse.created(response);
    }
}
