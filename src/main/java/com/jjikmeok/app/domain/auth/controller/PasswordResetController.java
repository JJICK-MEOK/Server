package com.jjikmeok.app.domain.auth.controller;

import com.jjikmeok.app.domain.auth.dto.request.PasswordResetReq;
import com.jjikmeok.app.domain.auth.dto.request.PasswordResetSendReq;
import com.jjikmeok.app.domain.auth.dto.request.PasswordResetVerifyReq;
import com.jjikmeok.app.domain.auth.dto.response.PasswordResetRes;
import com.jjikmeok.app.domain.auth.dto.response.PasswordResetSendRes;
import com.jjikmeok.app.domain.auth.dto.response.PasswordResetVerifyRes;
import com.jjikmeok.app.domain.auth.service.PasswordResetService;
import com.jjikmeok.app.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "비밀번호 재설정", description = "비밀번호 재설정 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "비밀번호 재설정 인증번호 발송",
            description = "가입된 로컬 계정 이메일인 경우 비밀번호 재설정 인증번호를 발송합니다."
    )
    @PostMapping("/send-code")
    public ApiResponse<PasswordResetSendRes> sendResetCode(
            @Valid @RequestBody final PasswordResetSendReq request
    ) {
        final PasswordResetSendRes response = passwordResetService.sendResetCode(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "비밀번호 재설정 인증번호 검증",
            description = "비밀번호 재설정 인증번호를 검증한 뒤 1회용 resetToken을 발급합니다."
    )
    @PostMapping("/verify-code")
    public ApiResponse<PasswordResetVerifyRes> verifyResetCode(
            @Valid @RequestBody final PasswordResetVerifyReq request
    ) {
        final PasswordResetVerifyRes response = passwordResetService.verifyResetCode(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "1회용 resetToken을 검증한 뒤 새 비밀번호로 변경합니다."
    )
    @PostMapping("/reset")
    public ApiResponse<PasswordResetRes> resetPassword(
            @Valid @RequestBody final PasswordResetReq request
    ) {
        final PasswordResetRes response = passwordResetService.resetPassword(request);
        return ApiResponse.success(response);
    }
}
