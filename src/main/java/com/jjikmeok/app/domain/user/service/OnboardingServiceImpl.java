package com.jjikmeok.app.domain.user.service;

import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.domain.user.converter.OnboardingConverter;
import com.jjikmeok.app.domain.user.dto.request.OnboardingReq;
import com.jjikmeok.app.domain.user.dto.response.OnboardingRes;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TagRepository tagRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserOnboardingRegionRepository userOnboardingRegionRepository;
    private final UserOnboardingTagRepository userOnboardingTagRepository;

    @Override
    @Transactional
    public OnboardingRes completeOnboarding(Long userId, OnboardingReq request) {
        User user = findUserOrThrow(userId);

        List<Long> topicTagIds = normalizeIds(request.topicTagIds());
        List<Long> regionIds = normalizeIds(request.regionIds());
        List<Long> preferenceTagIds = normalizeIds(request.preferenceTagIds());

        List<Region> regions = findRegionsOrThrow(regionIds);
        Map<Long, Tag> tagMap = findTagsOrThrow(topicTagIds, preferenceTagIds);

        UserOnboarding userOnboarding = findOrCreateUserOnboarding(user);

        replaceSelections(userOnboarding, regions, tagMap, topicTagIds, preferenceTagIds);

        user.completeOnboarding();

        return OnboardingConverter.toOnboardingResponse(user, userOnboarding, topicTagIds, regionIds, preferenceTagIds);
    }

    /**
     * 사용자의 온보딩 정보를 조회하고, 없으면 새로 생성한다.
     */
    private UserOnboarding findOrCreateUserOnboarding(User user) {
        return userOnboardingRepository.findByUserId(user.getId())
                .orElseGet(() -> userOnboardingRepository.save(
                        OnboardingConverter.toUserOnboarding(user)
                ));
    }

    /**
     * 사용자 ID로 사용자를 조회하고, 없으면 예외를 던진다.
     */
    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
    }

    /**
     * 입력 순서를 유지하면서 중복 ID를 제거한다.
     */
    private List<Long> normalizeIds(List<Long> ids) {
        return List.copyOf(new LinkedHashSet<>(ids));
    }

    /**
     * 요청한 지역 ID들이 모두 존재하는지 검증하고, 요청 순서대로 반환한다.
     */
    private List<Region> findRegionsOrThrow(List<Long> regionIds) {
        List<Region> regions = regionRepository.findAllByIdIn(regionIds);
        if (regions.size() != regionIds.size()) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        Map<Long, Region> regionMap = regions.stream()
                .collect(Collectors.toMap(Region::getId, Function.identity()));

        return regionIds.stream()
                .map(regionMap::get)
                .toList();
    }

    /**
     * 요청한 태그들을 조회하고, 존재 여부 및 태그 타입을 함께 검증한다.
     */
    private Map<Long, Tag> findTagsOrThrow(List<Long> topicTagIds, List<Long> preferenceTagIds) {
        List<Tag> topicTags = findTopicTagsOrThrow(topicTagIds);
        List<Tag> preferenceTags = findPreferenceTagsOrThrow(preferenceTagIds);

        return java.util.stream.Stream.concat(topicTags.stream(), preferenceTags.stream())
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
    }

    /**
     * 요청한 태그 ID 목록이 모두 기대한 타입인지 검증한다.
     */
    private List<Tag> findTopicTagsOrThrow(List<Long> topicTagIds) {
        List<Tag> tags = tagRepository.findAllByIdInAndType(topicTagIds, TagType.TOPIC_CATEGORY);
        if (tags.size() != topicTagIds.size()) {
            throw new CustomException(ErrorCode.ONBOARDING_INVALID_TOPIC_TAG_TYPE);
        }

        return tags;
    }

    private List<Tag> findPreferenceTagsOrThrow(List<Long> preferenceTagIds) {
        List<Tag> tags = tagRepository.findAllByIdInAndType(preferenceTagIds, TagType.PREFERENCE_TAG);
        if (tags.size() != preferenceTagIds.size()) {
            throw new CustomException(ErrorCode.ONBOARDING_INVALID_PREFERENCE_TAG_TYPE);
        }

        return tags;
    }

    /**
     * 기존 온보딩 선택값을 모두 지우고, 현재 요청값으로 전체 교체한다.
     */
    private void replaceSelections(
            UserOnboarding userOnboarding,
            List<Region> regions,
            Map<Long, Tag> tagMap,
            List<Long> topicTagIds,
            List<Long> preferenceTagIds
    ) {
        userOnboardingRegionRepository.deleteAllByUserOnboardingId(userOnboarding.getId());
        userOnboardingTagRepository.deleteAllByUserOnboardingId(userOnboarding.getId());

        userOnboardingRegionRepository.saveAll(
                regions.stream()
                        .map(region -> OnboardingConverter.toUserOnboardingRegion(userOnboarding, region))
                        .toList()
        );

        List<UserOnboardingTag> onboardingTags = topicTagIds.stream()
                .map(tagMap::get)
                .map(tag -> OnboardingConverter.toUserOnboardingTag(userOnboarding, tag))
                .collect(Collectors.toCollection(ArrayList::new));

        onboardingTags.addAll(
                preferenceTagIds.stream()
                        .map(tagMap::get)
                        .map(tag -> OnboardingConverter.toUserOnboardingTag(userOnboarding, tag))
                        .toList()
        );

        userOnboardingTagRepository.saveAll(onboardingTags);
    }
}
