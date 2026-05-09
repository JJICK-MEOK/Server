package com.jjikmeok.app.domain.auth.service;

import com.jjikmeok.app.domain.auth.dto.request.SignupReq;
import com.jjikmeok.app.domain.auth.dto.response.SignupRes;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupRes signup(SignupReq request) {

        final String email = request.getEmail();
        validateEmailNotExists(email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.createForSignup(
                email,
                encodedPassword
        );

        final User saved = saveUserOrThrowDuplicateEmail(user, email);

        log.info("회원가입 완료 - email: {}, userId: {}", saved.getEmail(), saved.getId());
        return new SignupRes(saved.getId(), saved.getEmail());
    }

    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

    private User saveUserOrThrowDuplicateEmail(final User user, final String email) {
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.debug("회원가입 저장 중 이메일 중복(유니크 제약)으로 실패했습니다. email={}", email);
            throw new CustomException(ErrorCode.SIGNUP_FAILED);
        }
    }

}