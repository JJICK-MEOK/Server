package com.jjikmeok.app.domain.onboarding.service.query;

import com.jjikmeok.app.domain.onboarding.converter.OnboardingConverter;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingPreferenceTagRes;
import com.jjikmeok.app.domain.onboarding.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboarding;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingRegion;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.onboarding.repository.query.UserOnboardingQueryRepository;
import com.jjikmeok.app.domain.onboarding.repository.query.UserOnboardingRegionQueryRepository;
import com.jjikmeok.app.domain.onboarding.repository.query.UserOnboardingTagQueryRepository;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingQueryServiceImpl implements OnboardingQueryService {

    private final UserRepository userRepository;
    private final UserOnboardingQueryRepository userOnboardingQueryRepository;
    private final UserOnboardingTagQueryRepository userOnboardingTagQueryRepository;
    private final UserOnboardingRegionQueryRepository userOnboardingRegionQueryRepository;
    private final TagRepository tagRepository;

    @Override
    public OnboardingRes getOnboarding(Long userId) {
        User user = findUserOrThrow(userId);

        UserOnboarding userOnboarding = userOnboardingQueryRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("온보딩 조회 실패 - 온보딩 정보를 찾을 수 없습니다. userId={}", userId);
                    return new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
                });

        List<UserOnboardingTag> onboardingTags =
                userOnboardingTagQueryRepository.findAllByUserOnboardingIdOrderByIdAsc(userOnboarding.getId());
        List<UserOnboardingRegion> onboardingRegions =
                userOnboardingRegionQueryRepository.findAllByUserOnboardingIdOrderByIdAsc(userOnboarding.getId());

        List<Long> topicTagIds = extractTagIdsByType(onboardingTags, TagType.TOPIC_CATEGORY);
        List<Long> preferenceTagIds = extractTagIdsByType(onboardingTags, TagType.PREFERENCE_TAG);
        List<Long> regionIds = onboardingRegions.stream()
                .map(onboardingRegion -> onboardingRegion.getRegion().getId())
                .toList();

        log.debug("온보딩 조회 완료. userId={}, onboardingId={}", user.getId(), userOnboarding.getId());
        return OnboardingConverter.toOnboardingResponse(user, userOnboarding, topicTagIds, regionIds, preferenceTagIds);
    }

    @Override
    public List<OnboardingPreferenceTagRes> getPreferenceTagsForEdit(Long userId) {
        findUserOrThrow(userId);

        Set<Long> selectedTagIds = new HashSet<>(userOnboardingTagQueryRepository.findTagIdsByUserId(userId));

        List<OnboardingPreferenceTagRes> response = tagRepository.findAllByTypeOrderByNameAsc(TagType.PREFERENCE_TAG).stream()
                .map(tag -> OnboardingConverter.toOnboardingPreferenceTagRes(tag, selectedTagIds.contains(tag.getId())))
                .toList();

        log.debug("온보딩 취향 태그 조회 완료. userId={}, tagCount={}", userId, response.size());
        return response;
    }

    /**
     * 온보딩 태그 목록에서 지정한 태그 유형에 해당하는 태그 ID를 추출한다.
     **/
    private List<Long> extractTagIdsByType(List<UserOnboardingTag> onboardingTags, TagType tagType) {
        return onboardingTags.stream()
                .filter(onboardingTag -> onboardingTag.getTag().getType() == tagType)
                .map(onboardingTag -> onboardingTag.getTag().getId())
                .toList();
    }

    /**
     * 사용자 ID로 사용자를 조회하고, 인증 정보가 없거나 사용자를 찾을 수 없으면 예외를 발생시킨다.
     **/
    private User findUserOrThrow(Long userId) {
        if (userId == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다. userId={}", userId);
                    return new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
                });
    }
}
