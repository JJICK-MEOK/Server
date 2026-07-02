package com.jjikmeok.app.domain.auth.service;

import com.jjikmeok.app.domain.auth.dto.request.LoginReq;
import com.jjikmeok.app.domain.auth.dto.request.ReissueReq;
import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.LoginRes;
import com.jjikmeok.app.domain.auth.dto.response.ReissueRes;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.auth.store.HandoffTokenStore;
import com.jjikmeok.app.domain.auth.store.RefreshTokenStore;
import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.security.exception.JwtTokenException;
import com.jjikmeok.app.global.security.jwt.JwtProperties;
import com.jjikmeok.app.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final HandoffTokenStore handoffTokenStore;

    /**
     * 이메일 회원가입
     */
    @Transactional
    public SignupRes signup(final SignupReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        validateEmailNotExists(email);

        final String encodedPassword = passwordEncoder.encode(request.password());
        final User user = User.createForSignup(email, encodedPassword);
        final User saved = saveUserOrThrowDuplicateEmail(user, email);

        log.info("회원가입이 완료되었습니다.  userId={}", saved.getId());
        return new SignupRes(saved.getId(), saved.getEmail());
    }

    /**
     * 이메일 로그인
     */
    @Transactional
    public LoginRes login(final LoginReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        final User user = findUserOrThrowInvalidCredentials(email);

        validateLocalLogin(user);
        validatePasswordMatches(request.password(), user.getPasswordHash());

        return issueLoginTokens(user);
    }

    /**
     * accessToken 만료시, refreshToken 으로 accessToken 재발행
     */
    @Transactional
    public ReissueRes reissue(final ReissueReq request) {
        final String refreshToken = request.refreshToken();
        final Long userId = parseRefreshTokenUserId(refreshToken);

        if (!refreshTokenStore.matches(userId, refreshToken)) {
            log.warn("토큰 재발급 실패 - 저장된 리프레시 토큰과 일치하지 않습니다. userId={}", userId);
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        final User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("토큰 재발급 실패 - 사용자를 찾을 수 없습니다. userId={}", userId);
                    return new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
                });

        return rotateAndIssueTokens(user);
    }

    /**
     * 소셜 로그인시, 클라이언트가 서버로 부터 받은 handoffToken 을 통해서 accessToken, refreshToken 을 발급
     */
    @Transactional
    public LoginRes exchangeHandoffToken(final String handoffToken) {
        final HandoffTokenEntry entry = handoffTokenStore.consume(handoffToken)
                .orElseThrow(() -> {
                    log.warn("Handoff 토큰 교환 실패 - 유효하지 않은 handoff 토큰입니다.");
                    return new CustomException(ErrorCode.AUTH_HANDOFF_TOKEN_INVALID);
                });
        final User user = userRepository.findById(entry.memberId())
                .orElseThrow(() -> {
                    log.warn("Handoff 토큰 교환 실패 - 사용자를 찾을 수 없습니다. userId={}", entry.memberId());
                    return new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
                });

        return issueLoginTokens(user);
    }

    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("회원가입 실패 - 이미 사용 중인 이메일입니다. email={}", email);
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

    private User findUserOrThrowInvalidCredentials(final String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 가입되지 않은 이메일입니다. email={}", email);
                    return new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });
    }

    private User saveUserOrThrowDuplicateEmail(final User user, final String email) {
        try {
            return userRepository.save(user);
        } catch (final DataIntegrityViolationException e) {
            log.warn("회원가입 실패 - DB 제약조건 위반으로 이메일 중복이 감지되었습니다. email={}", email);
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

    private void validateLocalLogin(final User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            log.warn("로그인 실패 - 로컬 계정이 아닙니다. userId={}, provider={}", user.getId(), user.getAuthProvider());
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private void validatePasswordMatches(final String rawPassword, final String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("로그인 실패 - 비밀번호가 일치하지 않습니다.");
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    private LoginRes issueLoginTokens(final User user) {
        final Long userId = user.getId();
        final String role = AuthUtils.resolveRole(user.getRole());
        final String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        final String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        final int expiresIn = AuthUtils.accessTokenExpiresInSeconds(jwtProperties);

        refreshTokenStore.saveToken(userId, refreshToken, AuthUtils.refreshTokenTtl(jwtProperties));

        log.debug("로그인 토큰 발급 완료. userId={}", userId);
        return new LoginRes(accessToken, refreshToken, TOKEN_TYPE, expiresIn, user.getRegistrationStatus());
    }

    private ReissueRes rotateAndIssueTokens(final User user) {
        final Long userId = user.getId();
        final String role = AuthUtils.resolveRole(user.getRole());
        final String accessToken = jwtTokenProvider.createAccessToken(userId, role);
        final String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        final int expiresIn = AuthUtils.accessTokenExpiresInSeconds(jwtProperties);

        storeRotatedRefreshToken(userId, refreshToken);

        log.debug("토큰 재발급 완료. userId={}", userId);
        return new ReissueRes(accessToken, refreshToken, TOKEN_TYPE, expiresIn);
    }

    private void storeRotatedRefreshToken(final Long userId, final String refreshToken) {
        refreshTokenStore.saveToken(userId, refreshToken, AuthUtils.refreshTokenTtl(jwtProperties));
    }

    private Long parseRefreshTokenUserId(final String refreshToken) {
        try {
            return jwtTokenProvider.getUserId(refreshToken);
        } catch (final JwtTokenException e) {
            if (e.getErrorCode() == ErrorCode.JWT_EXPIRED_TOKEN) {
                log.warn("토큰 재발급 실패 - 리프레시 토큰이 만료되었습니다.");
                throw new CustomException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
            }
            log.warn("토큰 재발급 실패 - 유효하지 않은 리프레시 토큰입니다. errorCode={}", e.getErrorCode());
            throw new CustomException(e.getErrorCode());
        }
    }
}
