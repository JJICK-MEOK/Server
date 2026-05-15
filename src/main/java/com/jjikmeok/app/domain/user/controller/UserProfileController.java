package com.jjikmeok.app.domain.user.controller;

import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;
import com.jjikmeok.app.domain.user.service.UserProfileService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/profile")
@Tag(name = "User Profile", description = "사용자 프로필 API")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "사용자 프로필 생성", description = "로그인한 사용자의 프로필 정보를 최초 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileCreateRes> createProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileCreateReq request
    ) {
        UserProfileCreateRes response = userProfileService.createProfile(userId, request);
        return ApiResponse.success(response);
    }
}
