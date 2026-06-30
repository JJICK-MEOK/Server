package com.jjikmeok.app.domain.auth.service;

import java.security.SecureRandom;
import java.time.Duration;

import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationVerifyReq;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationSendRes;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationVerifyRes;
import com.jjikmeok.app.domain.auth.store.VerificationCodeStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationSendReq;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.infra.mail.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(3);
    private static final int VERIFICATION_CODE_BOUND = 1_000_000;

    private final VerificationCodeStore verificationCodeStore;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public EmailVerificationSendRes sendVerificationCode(final EmailVerificationSendReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());

        validateEmailNotExists(email);

        final String code = generateVerificationCode();
        verificationCodeStore.saveCode(email, code, VERIFICATION_CODE_TTL);

        final String html = buildVerificationMailHtml(code);

        mailService.sendHtml(
                email,
                "[찍먹] 이메일 인증번호 안내",
                html
        );

        log.info("이메일 인증번호 발송 요청 완료. email={}", email);

        return new EmailVerificationSendRes(
                email,
                (int) VERIFICATION_CODE_TTL.toSeconds()
        );
    }

    @Transactional
    public EmailVerificationVerifyRes verifyVerificationCode(final EmailVerificationVerifyReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());

        validateEmailNotExists(email);

        final String savedCode = verificationCodeStore.getCode(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_VERIFICATION_CODE_EXPIRED));

        if (!savedCode.equals(request.code())) {
            throw new CustomException(ErrorCode.MAIL_VERIFICATION_CODE_INVALID);
        }

        verificationCodeStore.deleteCode(email);
        log.info("이메일 인증번호 검증 완료. email={}", email);

        return new EmailVerificationVerifyRes(email, true);
    }

    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.RESOURCE_CONFLICT);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(VERIFICATION_CODE_BOUND));
    }

    private String buildVerificationMailHtml(final String code) {
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>찍먹 이메일 인증번호</h2>
                    <p>회원가입을 계속하려면 아래 인증번호를 입력해주세요.</p>
                    <div style="font-size: 24px; font-weight: bold; letter-spacing: 4px; margin: 20px 0;">
                        %s
                    </div>
                    <p>본 인증번호는 3분간 유효합니다.</p>
                    <p>요청하지 않았다면 본 메일을 무시해주세요.</p>
                </div>
                """.formatted(code);
    }
}
