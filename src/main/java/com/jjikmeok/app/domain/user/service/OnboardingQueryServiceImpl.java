package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.domain.user.dto.response.OnboardingPreferenceTagResponse;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.entity.RegistrationStatus;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import com.jjikmeok.app.domain.user.entity.UserOnboardingRegion;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.repository.UserOnboardingQueryRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionQueryRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagQueryRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class OnboardingQueryServiceImpl implements OnboardingQueryService {

    private final UserRepository userRepository;
    private final UserOnboardingQueryRepository userOnboardingQueryRepository;
    private final UserOnboardingTagQueryRepository userOnboardingTagQueryRepository;
    private final UserOnboardingRegionQueryRepository userOnboardingRegionQueryRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public OnboardingRes getOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        UserOnboarding userOnboarding = userOnboardingQueryRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        List<UserOnboardingTag> onboardingTags =
                userOnboardingTagQueryRepository.findAllByUserOnboardingIdOrderByIdAsc(userOnboarding.getId());
        List<UserOnboardingRegion> onboardingRegions =
                userOnboardingRegionQueryRepository.findAllByUserOnboardingIdOrderByIdAsc(userOnboarding.getId());

        List<Long> topicTagIds = extractTagIdsByType(onboardingTags, TagType.TOPIC_CATEGORY);
        List<Long> preferenceTagIds = extractTagIdsByType(onboardingTags, TagType.PREFERENCE_TAG);
        List<Long> regionIds = onboardingRegions.stream()
                .map(onboardingRegion -> onboardingRegion.getRegion().getId())
                .toList();

        return new OnboardingRes(
                user.getId(),
                userOnboarding.getId(),
                user.getRegistrationStatus() == RegistrationStatus.ONBOARDING_COMPLETED,
                topicTagIds,
                regionIds,
                preferenceTagIds
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingPreferenceTagResponse> getPreferenceTagsForEdit(Long userId) {
        validateUser(userId);

        Set<Long> selectedTagIds = new HashSet<>(userOnboardingTagQueryRepository.findTagIdsByUserId(userId));

        return tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG).stream()
                .map(tag -> new OnboardingPreferenceTagResponse(
                        tag.getId(),
                        tag.getName(),
                        tag.getType(),
                        selectedTagIds.contains(tag.getId())
                ))
                .toList();
    }

    private List<Long> extractTagIdsByType(List<UserOnboardingTag> onboardingTags, TagType tagType) {
        return onboardingTags.stream()
                .filter(onboardingTag -> onboardingTag.getTag().getType() == tagType)
                .map(onboardingTag -> onboardingTag.getTag().getId())
                .toList();
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }
}
