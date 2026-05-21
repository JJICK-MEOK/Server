package com.jjikmeok.app.domain.auth.controller;

import com.jjikmeok.app.domain.auth.dto.request.LoginReq;
import com.jjikmeok.app.domain.auth.dto.request.ReissueReq;
import com.jjikmeok.app.domain.auth.dto.request.SocialLoginReq;
import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.LoginRes;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.auth.service.AuthService;
import com.jjikmeok.app.domain.auth.service.GoogleAuthService;
import com.jjikmeok.app.domain.auth.service.KakaoAuthService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;

    @Operation(
            summary = "회원가입",
            description = "이메일과 비밀번호로 로컬 계정을 생성합니다."
    )
    @PostMapping("/signup")
    public ApiResponse<SignupRes> signup(@Valid @RequestBody final SignupReq request) {
        final SignupRes response = authService.signup(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고 access token과 refresh token을 발급합니다."
    )
    @PostMapping("/login")
    public ApiResponse<LoginRes> login(@Valid @RequestBody final LoginReq request) {
        final LoginRes response = authService.login(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Request Body의 Refresh Token을 검증한 뒤 Access Token과 Refresh Token을 재발급합니다."
    )
    @PostMapping("/reissue")
    public ApiResponse<LoginRes> reissue(@Valid @RequestBody final ReissueReq request) {
        final LoginRes response = authService.reissue(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "구글 로그인",
            description = "구글 인가 코드를 받아 소셜 로그인 후 access token과 refresh token을 발급합니다."
    )
    @PostMapping("/google/login")
    public ApiResponse<LoginRes> googleLogin(@Valid @RequestBody final SocialLoginReq request) {
        final LoginRes response = googleAuthService.googleLogin(request.code());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 인가 코드를 받아 소셜 로그인 후 access token과 refresh token을 발급합니다."
    )
    @PostMapping("/kakao/login")
    public ApiResponse<LoginRes> kakaoLogin(@Valid @RequestBody final SocialLoginReq request) {
        final LoginRes response = kakaoAuthService.kakaoLogin(request.code());
        return ApiResponse.success(response);
    }
}
