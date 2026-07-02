package com.jjikmeok.app.domain.auth.controller;

import java.net.URI;

import com.jjikmeok.app.domain.auth.service.GoogleOAuthHandoffService;
import com.jjikmeok.app.domain.auth.service.KakaoOAuthHandoffService;
import com.jjikmeok.app.domain.auth.service.NaverOAuthHandoffService;
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
@Tag(name = "OAuth", description = "서버 주도 OAuth 로그인 API")
public class OAuthController {

    private final GoogleOAuthHandoffService googleOAuthHandoffService;
    private final KakaoOAuthHandoffService kakaoOAuthHandoffService;
    private final NaverOAuthHandoffService naverOAuthHandoffService;

    @Operation(
            summary = "구글 OAuth 로그인 시작",
            description = "CSRF 방지 state를 생성하고 구글 OAuth 인증 URL로 리다이렉트합니다."
    )
    @GetMapping("/google/login")
    public ResponseEntity<Void> googleLogin() {
        final URI redirectUri = googleOAuthHandoffService.createGoogleLoginUri();
        return redirect(redirectUri);
    }

    @Operation(
            summary = "구글 OAuth 콜백",
            description = "구글 인가 코드를 처리하고 handoff 토큰이 포함된 앱 딥링크로 리다이렉트합니다."
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
            summary = "카카오 OAuth 로그인 시작",
            description = "CSRF 방지 state를 생성하고 카카오 OAuth 인증 URL로 리다이렉트합니다."
    )
    @GetMapping("/kakao/login")
    public ResponseEntity<Void> kakaoLogin() {
        final URI redirectUri = kakaoOAuthHandoffService.createKakaoLoginUri();
        return redirect(redirectUri);
    }

    @Operation(
            summary = "카카오 OAuth 콜백",
            description = "카카오 인가 코드를 처리하고 handoff 토큰이 포함된 앱 딥링크로 리다이렉트합니다."
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

    @Operation(
            summary = "네이버 OAuth 로그인 시작",
            description = "CSRF 방지 state를 생성하고 네이버 OAuth 인증 URL로 리다이렉트합니다."
    )
    @GetMapping("/naver/login")
    public ResponseEntity<Void> naverLogin() {
        final URI redirectUri = naverOAuthHandoffService.createNaverLoginUri();
        return redirect(redirectUri);
    }

    @Operation(
            summary = "네이버 OAuth 콜백",
            description = "네이버 인가 코드를 처리하고 handoff 토큰을 포함한 앱 딥링크로 리다이렉트합니다."
    )
    @GetMapping("/naver/callback")
    public ResponseEntity<Void> naverCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error
    ) {
        final URI appDeepLinkUri = naverOAuthHandoffService.handleNaverCallback(code, state, error);
        return redirect(appDeepLinkUri);
    }

    private ResponseEntity<Void> redirect(final URI location) {
        return ResponseEntity
                .status(302)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }
}
