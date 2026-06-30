package com.jjikmeok.app.domain.auth.service;

import java.time.Duration;
import java.time.Instant;

import com.jjikmeok.app.domain.auth.store.HandoffTokenStore;
import com.jjikmeok.app.domain.auth.token.HandoffTokenEntry;
import com.jjikmeok.app.domain.auth.token.OAuthTokenGenerator;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthHandoffCommonService {

    private final UserRepository userRepository;
    private final HandoffTokenStore handoffTokenStore;
    private final OAuthTokenGenerator oAuthTokenGenerator;

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
                    final User savedUser = userRepository.save(User.createForOAuth2(email, provider, providerId));
                    log.info("{} OAuth handoff signup completed. userId={}, providerId={}",
                            provider, savedUser.getId(), providerId);
                    return new OAuthUserResult(savedUser, true);
                });
    }

    public String createHandoffToken(
            final OAuthUserResult userResult,
            final int handoffTokenBytes,
            final Duration handoffTtl
    ) {
        final String handoffToken = oAuthTokenGenerator.generateUrlSafeToken(handoffTokenBytes);
        final HandoffTokenEntry entry = new HandoffTokenEntry(
                userResult.user().getId(),
                userResult.newMember(),
                Instant.now()
        );
        handoffTokenStore.save(handoffToken, entry, handoffTtl);
        return handoffToken;
    }
}
