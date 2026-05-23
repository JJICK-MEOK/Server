package com.jjikmeok.app.domain.auth.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjikmeok.app.domain.auth.dto.request.LoginReq;
import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.LoginRes;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.auth.store.RefreshTokenStore;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.security.jwt.JwtProperties;
import com.jjikmeok.app.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public SignupRes signup(final SignupReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        validateEmailNotExists(email);

        final String encodedPassword = passwordEncoder.encode(request.password());
        final User user = User.createForSignup(email, encodedPassword);
        final User saved = saveUserOrThrowDuplicateEmail(user, email);

        log.info("회원가입 완료 - email: {}, userId: {}", saved.getEmail(), saved.getId());
        return new SignupRes(saved.getId(), saved.getEmail());
    }

    @Transactional
    public LoginRes login(final LoginReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        final User user = findUserOrThrowInvalidCredentials(email);

        validateLocalLogin(user);
        validatePasswordMatches(request.password(), user.getPasswordHash());

        final Long userId = user.getId();
        final String role = AuthUtils.resolveRole(user.getRole());
        final String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        final String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        final int expiresIn = AuthUtils.accessTokenExpiresInSeconds(jwtProperties);

        refreshTokenStore.saveToken(userId, refreshToken, AuthUtils.refreshTokenTtl(jwtProperties));

        return new LoginRes(accessToken, refreshToken, TOKEN_TYPE, expiresIn);
    }

    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

    private User findUserOrThrowInvalidCredentials(final String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    private User saveUserOrThrowDuplicateEmail(final User user, final String email) {
        try {
            return userRepository.save(user);
        } catch (final DataIntegrityViolationException e) {
            log.debug("회원가입 저장 중 이메일 중복으로 실패했습니다. email={}", email);
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

    private void validateLocalLogin(final User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private void validatePasswordMatches(final String rawPassword, final String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }
}
