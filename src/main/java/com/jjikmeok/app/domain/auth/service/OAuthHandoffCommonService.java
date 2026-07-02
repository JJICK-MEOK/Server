package com.jjikmeok.app.domain.auth.service;

import java.time.Duration;
import java.time.Instant;

import com.jjikmeok.app.domain.auth.store.HandoffTokenStore;
import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;
import com.jjikmeok.app.domain.auth.token.SecureTokenGenerator;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthHandoffCommonService {

    private final UserRepository userRepository;
    private final HandoffTokenStore handoffTokenStore;
    private final SecureTokenGenerator secureTokenGenerator;

    @Transactional
    public OAuthUserResult findOrCreateUser(
            final AuthProvider provider,
            final String providerId,
            final String rawEmail
    ) {
        final String email = AuthUtils.normalizeEmail(rawEmail);

        return userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .map(user -> new OAuthUserResult(user, false))
                .orElseGet(() -> {
                    validateEmailNotConflicting(provider, providerId, email);
                    final User savedUser = saveOAuthUserOrThrowEmailConflict(provider, providerId, email);
                    return new OAuthUserResult(savedUser, true);
                });
    }

    public String createHandoffToken(
            final OAuthUserResult userResult,
            final int handoffTokenBytes,
            final Duration handoffTtl
    ) {
        final String handoffToken = secureTokenGenerator.generateUrlSafeToken(handoffTokenBytes);
        final HandoffTokenEntry entry = new HandoffTokenEntry(
                userResult.user().getId(),
                userResult.newMember(),
                Instant.now()
        );
        handoffTokenStore.save(handoffToken, entry, handoffTtl);
        return handoffToken;
    }

    /**
     * 소셜 회원가입 시 동일 이메일을 사용하는 기존 계정이 있는지 검증한다.
     */
    private void validateEmailNotConflicting(
            final AuthProvider provider,
            final String providerId,
            final String email
    ) {
        if (email == null) {
            return;
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("{} OAuth 회원가입 차단: 이미 사용 중인 이메일입니다. providerId={}, email={}",
                    provider, providerId, email);
            throw new CustomException(ErrorCode.AUTH_SOCIAL_EMAIL_CONFLICT);
        }
    }

    /**
     * OAuth 사용자를 저장하고 DB 제약조건 위반 시 이메일 충돌 예외로 변환한다.
     */
    private User saveOAuthUserOrThrowEmailConflict(
            final AuthProvider provider,
            final String providerId,
            final String email
    ) {
        try {
            final User savedUser = userRepository.save(User.createForOAuth2(email, provider, providerId));
            log.info("{} OAuth handoff 회원가입 완료: userId={}, providerId={}",
                    provider, savedUser.getId(), providerId);
            return savedUser;
        } catch (final DataIntegrityViolationException e) {
            return resolveSaveConflict(provider, providerId, email, e);
        }
    }

    /**
     *  동일한 provider/providerId 계정이 이미 생성되어 있는지 확인한다.
     *  이미 생성된 계정이 있으면 회원가입 경합 상황으로 판단하고 기존 User를 반환한다.
     *  기존 provider 계정이 없고 이메일이 중복된 경우, 소셜 이메일 충돌 예외를 발생.
     *  providerId 중복이나 이메일 중복이 아닌 다른 DB 제약조건 위반이면 원본 예외를 다시 던진다.
     */
    private User resolveSaveConflict(
            final AuthProvider provider,
            final String providerId,
            final String email,
            final DataIntegrityViolationException exception
    ) {
        return userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .map(user -> {
                    log.warn("{} OAuth handoff 회원가입 경합 감지: 이미 생성된 계정을 사용합니다. userId={}, providerId={}",
                            provider, user.getId(),providerId);
                    return user;
                })
                .orElseGet(() -> {
                    if (email != null && userRepository.existsByEmail(email)) {
                        log.warn("{} OAuth 회원가입 차단: 이메일 중복 제약조건 위반이 발생했습니다. providerId={}, email={}",
                                provider, providerId, email);
                        throw new CustomException(ErrorCode.AUTH_SOCIAL_EMAIL_CONFLICT);
                    }

                    log.warn("{} OAuth 회원가입 실패: provider 계정 중복 또는 이메일 중복이 아닌 DB 제약조건 위반입니다. providerId={}, email={}",
                            provider, providerId, email, exception);
                    throw exception;
                });
    }
}
