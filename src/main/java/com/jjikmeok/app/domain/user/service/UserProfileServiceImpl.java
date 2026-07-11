package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.user.converter.UserProfileConverter;
import com.jjikmeok.app.domain.user.dto.request.UserProfileCreateReq;
import com.jjikmeok.app.domain.user.dto.response.UserProfileCreateRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileMeRes;
import com.jjikmeok.app.domain.user.dto.response.UserProfileTagProjection;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final int MY_PROFILE_TAG_LIMIT = 5;
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
             throw new CustomException(ErrorCode.RESOURCE_CONFLICT);
        }

        user.completeProfile();
        return new UserProfileCreateRes(user.getRegistrationStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileMeRes getMyProfile(Long userId) {
        Long authenticatedUserId = requireAuthenticatedUserId(userId);
        UserProfile userProfile = findProfileOrThrow(authenticatedUserId);
        List<UserProfileTagProjection> tags = findLimitedTags(authenticatedUserId);

        return UserProfileConverter.toMyProfileResponse(userProfile, tags);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
    }

    /**
     * 인증된 사용자 ID가 존재하는지 확인.
     */
    private Long requireAuthenticatedUserId(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 사용자 ID로 프로필을 조회하고 없으면 예외를 던진다.
     */
    private UserProfile findProfileOrThrow(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
    }

    /**
     * 마이 프로필 화면에 표시할 태그를 최대 표시 개수만큼 조회.
     */
    private List<UserProfileTagProjection> findLimitedTags(Long userId) {
        return userProfileRepository.findTagProjectionsByUserId(userId).stream()
                .limit(MY_PROFILE_TAG_LIMIT)
                .toList();
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
}
