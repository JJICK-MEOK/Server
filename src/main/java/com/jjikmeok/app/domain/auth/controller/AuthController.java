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
@Tag(name = "Auth", description = "Authentication API")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Signup",
            description = "Creates a local account with email and password."
    )
    @PostMapping("/signup")
    public ApiResponse<SignupRes> signup(@Valid @RequestBody final SignupReq request) {
        final SignupRes response = authService.signup(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a local account and issues access and refresh tokens."
    )
    @PostMapping("/login")
    public ApiResponse<LoginRes> login(@Valid @RequestBody final LoginReq request) {
        final LoginRes response = authService.login(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "Reissue tokens",
            description = "Validates a refresh token and reissues access and refresh tokens."
    )
    @PostMapping("/reissue")
    public ApiResponse<ReissueRes> reissue(@Valid @RequestBody final ReissueReq request) {
        final ReissueRes response = authService.reissue(request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "Exchange handoff token",
            description = "Consumes a one-time handoff token and issues service access and refresh tokens."
    )
    @PostMapping("/handoff")
    public ApiResponse<LoginRes> exchangeHandoffToken(@Valid @RequestBody final HandoffTokenReq request) {
        final LoginRes response = authService.exchangeHandoffToken(request.handoffToken());
        return ApiResponse.success(response);
    }
}
