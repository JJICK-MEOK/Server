package com.jjikmeok.app.domain.auth.service;

import java.time.Duration;

import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationSendReq;
import com.jjikmeok.app.domain.auth.dto.request.EmailVerificationVerifyReq;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationSendRes;
import com.jjikmeok.app.domain.auth.dto.response.EmailVerificationVerifyRes;
import com.jjikmeok.app.domain.auth.store.RedisVerificationCodeStore;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.infra.mail.MailService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(3);

    private final RedisVerificationCodeStore verificationCodeStore;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final VerificationCodeService verificationCodeService;

    @Transactional(readOnly = true)
    public EmailVerificationSendRes sendVerificationCode(final EmailVerificationSendReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());

        validateEmailNotExists(email);
        sendVerificationCodeMail(email);

        log.info("이메일 인증번호 발송 요청 완료. email={}", email);
        return new EmailVerificationSendRes(email, (int) VERIFICATION_CODE_TTL.toSeconds());
    }

    @Transactional
    public EmailVerificationVerifyRes verifyVerificationCode(final EmailVerificationVerifyReq request) {
        final String email = AuthUtils.normalizeEmail(request.email());

        validateEmailNotExists(email);

        verificationCodeService.verifyAndConsume(verificationCodeStore, email, request.code());
        log.info("이메일 인증번호 검증 완료. email={}", email);

        return new EmailVerificationVerifyRes(email, true);
    }

    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.RESOURCE_CONFLICT);
        }
    }

    private void sendVerificationCodeMail(final String email) {
        final String code = verificationCodeService.issueCode(
                verificationCodeStore,
                email,
                VERIFICATION_CODE_TTL
        );

        mailService.sendHtml(
                email,
                "[찍먹] 이메일 인증번호 안내",
                buildVerificationMailHtml(code)
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
        verificationCodeStore.getCode(email)
                .filter(code::equals)
                .ifPresent(currentCode -> verificationCodeStore.deleteCode(email));

        log.warn("이메일 인증번호 메일 발송에 실패했습니다. 현재 발급된 코드와 일치하는 경우 해당 코드를 무효화했습니다. email={}",
                email,
                throwable);
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
