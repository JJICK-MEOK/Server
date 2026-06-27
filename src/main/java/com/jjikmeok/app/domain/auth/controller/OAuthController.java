package com.jjikmeok.app.domain.auth.controller;

import java.net.URI;

import com.jjikmeok.app.domain.auth.service.GoogleOAuthHandoffService;
import com.jjikmeok.app.domain.auth.service.KakaoOAuthHandoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
@Tag(name = "OAuth", description = "Backend-driven OAuth login API")
public class OAuthController {

    private final GoogleOAuthHandoffService googleOAuthHandoffService;
    private final KakaoOAuthHandoffService kakaoOAuthHandoffService;

    @Operation(
            summary = "Start Google OAuth login",
            description = "Creates a CSRF state and redirects to the Google OAuth authorization URL."
    )
    @GetMapping("/google/login")
    public ResponseEntity<Void> googleLogin() {
        final URI redirectUri = googleOAuthHandoffService.createGoogleLoginUri();
        return redirect(redirectUri);
    }

    @Operation(
            summary = "Google OAuth callback",
            description = "Handles Google authorization code and redirects to the app deep link with a handoff token."
    )
    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error
    ) {
        final URI appDeepLinkUri = googleOAuthHandoffService.handleGoogleCallback(code, state, error);
        return redirect(appDeepLinkUri);
    }

    @Operation(
            summary = "Start Kakao OAuth login",
            description = "Creates a CSRF state and redirects to the Kakao OAuth authorization URL."
    )
    @GetMapping("/kakao/login")
    public ResponseEntity<Void> kakaoLogin() {
        final URI redirectUri = kakaoOAuthHandoffService.createKakaoLoginUri();
        return redirect(redirectUri);
    }

    @Operation(
            summary = "Kakao OAuth callback",
            description = "Handles Kakao authorization code and redirects to the app deep link with a handoff token."
    )
    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error
    ) {
        final URI appDeepLinkUri = kakaoOAuthHandoffService.handleKakaoCallback(code, state, error);
        return redirect(appDeepLinkUri);
    }

    private ResponseEntity<Void> redirect(final URI location) {
        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }
}
