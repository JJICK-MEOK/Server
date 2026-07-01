package com.jjikmeok.app.domain.auth.controller;

import com.jjikmeok.app.domain.auth.dto.request.HandoffTokenReq;
import com.jjikmeok.app.domain.auth.dto.request.LoginReq;
import com.jjikmeok.app.domain.auth.dto.request.ReissueReq;
import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.LoginRes;
import com.jjikmeok.app.domain.auth.dto.response.ReissueRes;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.auth.service.AuthService;
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
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

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
            description = "로컬 계정을 인증하고 액세스 토큰과 리프레시 토큰을 발급합니다."
    )
    @PostMapping("/login")
    public ApiResponse<LoginRes> login(@Valid @RequestBody final LoginReq request) {
        final LoginRes response = authService.login(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "리프레시 토큰을 검증하고 액세스 토큰과 리프레시 토큰을 재발급합니다."
    )
    @PostMapping("/reissue")
    public ApiResponse<ReissueRes> reissue(@Valid @RequestBody final ReissueReq request) {
        final ReissueRes response = authService.reissue(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "Handoff 토큰 교환",
            description = "1회용 handoff 토큰을 소비하고 서비스 액세스 토큰과 리프레시 토큰을 발급합니다."
    )
    @PostMapping("/handoff")
    public ApiResponse<LoginRes> exchangeHandoffToken(@Valid @RequestBody final HandoffTokenReq request) {
        final LoginRes response = authService.exchangeHandoffToken(request.handoffToken());
        return ApiResponse.success(response);
    }
}
