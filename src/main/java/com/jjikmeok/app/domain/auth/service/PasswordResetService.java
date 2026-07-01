package com.jjikmeok.app.domain.auth.service;

import java.time.Duration;

import com.jjikmeok.app.domain.auth.dto.request.PasswordResetReq;
import com.jjikmeok.app.domain.auth.dto.request.PasswordResetSendReq;
import com.jjikmeok.app.domain.auth.dto.response.PasswordResetRes;
import com.jjikmeok.app.domain.auth.dto.response.PasswordResetSendRes;
import com.jjikmeok.app.domain.auth.store.RedisPasswordResetCodeStore;
import com.jjikmeok.app.domain.user.entity.AuthProvider;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.infra.mail.MailService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final Duration PASSWORD_RESET_CODE_TTL = Duration.ofMinutes(3);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RedisPasswordResetCodeStore passwordResetCodeStore;
    private final VerificationCodeService verificationCodeService;

    @Transactional(readOnly = true)
    public PasswordResetSendRes sendResetCode(final PasswordResetSendReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        final User user = validateEmailExists(email);

        validateLocalAccount(user, email);
        sendResetCodeMail(email);

        log.debug("비밀번호 재설정 인증번호 발송 요청 처리 완료. email={}", email);

        return new PasswordResetSendRes(email, (int) PASSWORD_RESET_CODE_TTL.toSeconds());
    }

    @Transactional
    public PasswordResetRes resetPassword(final PasswordResetReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());
        final User user = validateEmailExists(email);

        validateLocalAccount(user, email);
        validatePasswordConfirm(email, request);

        verificationCodeService.verifyAndConsume(passwordResetCodeStore, email, request.code());
        user.changePassword(passwordEncoder.encode(request.newPassword()));

        log.debug("비밀번호 재설정 완료. email={}, userId={}", email, user.getId());

        return new PasswordResetRes(true);
    }

    private User validateEmailExists(final String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("비밀번호 재설정 요청 실패 - 가입되지 않은 이메일입니다. email={}", email);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * User 의 상태 (로컬 계정인지) 검증
     */
    private void validateLocalAccount(final User user, final String email) {
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            log.warn("비밀번호 재설정 요청 실패 - 로컬 계정이 아닙니다. email={}, provider={}", email, user.getAuthProvider());
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }

    /**
     * 비밀번호와 비밀번호 확인이 일치하는지 검증
     */
    private void validatePasswordConfirm(final String email, final PasswordResetReq request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            log.warn("비밀번호 재설정 요청 실패 - 새 비밀번호와 확인 값이 일치하지 않습니다. email={}", email);
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void sendResetCodeMail(final String email) {
        final String code = verificationCodeService.issueCode(
                passwordResetCodeStore,
                email,
                PASSWORD_RESET_CODE_TTL
        );

        mailService.sendHtml(
                email,
                "[찍먹] 비밀번호 재설정 인증번호 안내",
                buildPasswordResetMailHtml(code)
        ).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                deleteIssuedCodeIfCurrent(email, code, throwable);
            }
        });
    }

    /**
     * 실패했다면 해당 인증번호를 조건부로 삭제하고 로그를 남긴다 (보상 처리)
     */
    private void deleteIssuedCodeIfCurrent(final String email, final String code, final Throwable throwable) {
        verificationCodeService.deleteCodeIfCurrent(passwordResetCodeStore, email, code);

        log.warn("비밀번호 재설정 코드 메일 발송에 실패했습니다. 현재 발급된 코드와 일치하는 경우 해당 코드를 무효화했습니다. email={}",
                email,
                throwable);
    }

    private String buildPasswordResetMailHtml(final String code) {
        final long ttlMinutes = PASSWORD_RESET_CODE_TTL.toMinutes();
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>찍먹 비밀번호 재설정 인증번호</h2>
                    <p>비밀번호를 재설정하려면 아래 인증번호를 입력해주세요.</p>
                    <div style="font-size: 24px; font-weight: bold; letter-spacing: 4px; margin: 20px 0;">
                        %s
                    </div>
                    <p>본 인증번호는 %d분간 유효합니다.</p>
                    <p>요청하지 않았다면 본 메일을 무시해주세요.</p>
                </div>
                """.formatted(code, ttlMinutes);
    }
}
