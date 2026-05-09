package com.jjikmeok.app.domain.auth.controller;

import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationSendReq;
import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationVerifyReq;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationSendRes;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationVerifyRes;
import com.jjikmeok.app.domain.auth.service.EmailVerificationService;
import com.jjikmeok.app.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email Verification", description = "이메일 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "이메일 인증번호 발송",
            description = "회원가입 전 이메일 소유 여부를 확인하기 위해 입력한 이메일로 인증번호를 발송합니다."
    )
    @PostMapping("/send-code")
    public ApiResponse<EmailVerificationSendRes> sendVerificationCode(
            @Valid @RequestBody final EmailVerificationSendReq request
    ) {
        final EmailVerificationSendRes response = emailVerificationService.sendVerificationCode(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "이메일 인증번호 검증",
            description = "이메일로 발송된 인증번호를 검증합니다."
    )
    @PostMapping("/verify-code")
    public ApiResponse<EmailVerificationVerifyRes> verifyVerificationCode(
            @Valid @RequestBody final EmailVerificationVerifyReq request
    ) {
        final EmailVerificationVerifyRes response = emailVerificationService.verifyVerificationCode(request);
        return ApiResponse.success(response);
    }
}
