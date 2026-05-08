package com.jjikmeok.app.domain.auth.controller;

import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.auth.service.AuthService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<SignupRes> signup(@Valid @RequestBody SignupReq request) {
        final SignupRes response = authService.signup(request);
        return ApiResponse.success(response);
    }
}