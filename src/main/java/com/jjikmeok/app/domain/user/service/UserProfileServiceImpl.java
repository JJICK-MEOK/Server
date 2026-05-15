package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserProfile;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String NEXT_STEP_ONBOARDING = "ONBOARDING";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public UserProfileCreateRes createProfile(Long userId, UserProfileCreateReq request) {
        User user = findUserOrThrow(userId);

        String nickname = request.nickname().trim();

        validateProfileNotExists(userId);
        validateNicknameNotExists(nickname);
        validateRequiredTerms(request);

        UserProfile userProfile = UserProfile.create(
                user,
                nickname,
                request.birthDate(),
                request.gender(),
                request.status(),
                request.serviceTermsAgreed(),
                request.privacyPolicyAgreed(),
                request.marketingAgreed()
        );

        try {
            userProfileRepository.save(userProfile);
        } catch (DataIntegrityViolationException e) {
            handleDuplicateConstraint(userId, nickname);
        }

        user.completeProfile();
        return new UserProfileCreateRes(user.getRegistrationStatus());
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
    }

    private void validateProfileNotExists(Long userId) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }
    }

    private void validateNicknameNotExists(String nickname) {
        if (userProfileRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    private void validateRequiredTerms(UserProfileCreateReq request) {
        if (!Boolean.TRUE.equals(request.serviceTermsAgreed())
                || !Boolean.TRUE.equals(request.privacyPolicyAgreed())) {
            throw new CustomException(ErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }

    private void handleDuplicateConstraint(Long userId, String nickname) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }
        if (userProfileRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        throw new CustomException(ErrorCode.RESOURCE_CONFLICT);
    }
}
