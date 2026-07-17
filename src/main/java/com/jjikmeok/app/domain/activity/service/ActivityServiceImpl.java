package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.converter.ActivityConverter;
import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityServiceImpl implements ActivityService {

    private static final long MINIMUM_RECOMMENDATION_MATCHED_TAG_COUNT = 3L;
    private static final int RECOMMENDATION_LIMIT = 8;
    private static final ApprovalStatus RECOMMENDATION_APPROVAL_STATUS = ApprovalStatus.APPROVED;

    private final ActivityRepository activityRepository;
    private final RegionRepository regionRepository;
    private final ActivityTagAutoAttachService activityTagAutoAttachService;
    private final TagRepository tagRepository;

    @Override
    public List<ActivitySummaryResponse> getActivities(Long regionId, ActivityCategory category, ActivityType type, String keyword) {
        if (regionId != null && !regionRepository.existsById(regionId)) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        return activityRepository.findActiveActivitiesByFilters(regionId, category, type, keyword, LocalDate.now().atStartOfDay()).stream()
                .map(ActivityConverter::toSummaryResponse)
                .toList();
    }

    @Override
    public List<ActivitySummaryResponse> searchActivitiesByTags(List<Long> tagIds) {
        List<Long> normalizedTagIds = normalizeTagIds(tagIds);
        if (normalizedTagIds.isEmpty()) {
            return List.of();
        }

        return activityRepository.findActiveActivitiesByTagIds(
                        normalizedTagIds,
                        RECOMMENDATION_APPROVAL_STATUS,
                        LocalDate.now().atStartOfDay()
                ).stream()
                .map(ActivityConverter::toSummaryResponse)
                .toList();
    }

    @Override
    public List<ActivitySummaryResponse> getRecommendedActivities(Long userId) {
        return activityRepository.findRecommendedActivitiesByUserTags(
                        userId,
                        MINIMUM_RECOMMENDATION_MATCHED_TAG_COUNT,
                        RECOMMENDATION_APPROVAL_STATUS,
                        LocalDate.now().atStartOfDay(),
                        PageRequest.of(0, RECOMMENDATION_LIMIT)
                ).stream()
                .map(recommendation -> ActivityConverter.toSummaryResponse(
                        recommendation.activity(),
                        recommendation.liked()
                ))
                .toList();
    }

    @Override
    @Transactional
    public ActivityDetailResponse getActivity(Long activityId) {
        LocalDateTime recruitCutoff = LocalDate.now().atStartOfDay();
        int updatedCount = activityRepository.incrementViewCount(activityId, recruitCutoff);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        Activity activity = activityRepository.findOpenByIdWithRegion(activityId, recruitCutoff)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        return ActivityConverter.toDetailResponse(activity);
    }

    @Override
    @Transactional
    public ActivityDetailResponse createActivity(ActivityRequest request) {
        Activity savedActivity = createAndSaveActivity(request);
        activityTagAutoAttachService.refresh(savedActivity);

        return ActivityConverter.toDetailResponse(savedActivity);
    }

    @Override
    @Transactional
    public ActivityDetailResponse createActivityWithTags(ActivityRequest request, List<Long> tagIds) {
        Activity savedActivity = createAndSaveActivity(request);
        savedActivity.replaceTags(resolvePreferenceTags(tagIds));
        return ActivityConverter.toDetailResponse(savedActivity);
    }

    @Override
    @Transactional
    public ActivityDetailResponse updateActivity(Long id, ActivityRequest request) {
        validateActivityRequest(request);

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        activity.update(
                region,
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                request.sourceUrl(),
                request.address(),
                request.organizer(),
                request.contactInfo(),
                request.target(),
                request.startAt(),
                request.endAt(),
                request.recruitStartAt(),
                request.recruitEndAt(),
                request.price(),
                request.activityType(),
                request.category(),
                request.sourceType(),
                request.externalId(),
                request.approvalStatus(),
                request.isActive()
        );
        activityTagAutoAttachService.refresh(activity);

        return ActivityConverter.toDetailResponse(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        activity.deactivate();
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }

        LinkedHashSet<Long> uniqueTagIds = new LinkedHashSet<>(tagIds);
        uniqueTagIds.remove(null);
        return List.copyOf(uniqueTagIds);
    }

    private Activity createAndSaveActivity(ActivityRequest request) {
        validateActivityRequest(request);

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        Activity activity = ActivityConverter.toEntity(request, region);
        return activityRepository.save(activity);
    }

    private List<Tag> resolvePreferenceTags(List<Long> tagIds) {
        List<Long> normalizedTagIds = normalizeTagIds(tagIds);
        if (normalizedTagIds.size() != 6) {
            throw new IllegalArgumentException("시트 활동은 서로 다른 6개의 태그가 필요합니다.");
        }

        Map<Long, Tag> tagsById = tagRepository.findAllByIdInAndType(normalizedTagIds, TagType.PREFERENCE_TAG).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        if (tagsById.size() != normalizedTagIds.size()) {
            throw new CustomException(ErrorCode.TAG_NOT_FOUND);
        }

        return normalizedTagIds.stream()
                .map(tagsById::get)
                .toList();
    }

    private void validateActivityRequest(ActivityRequest request) {
        validateRecruitPeriod(request);
        validateActivityPeriod(request);
        validateRecruitBeforeActivity(request);
        validateUri(request.sourceUrl());
    }

    private void validateRecruitPeriod(ActivityRequest request) {
        if (request.recruitStartAt() != null && request.recruitStartAt().isAfter(request.recruitEndAt())) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_RECRUIT_PERIOD);
        }
    }

    private void validateActivityPeriod(ActivityRequest request) {
        if (request.startAt() != null
                && request.endAt() != null
                && request.startAt().isAfter(request.endAt())) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_ACTIVITY_PERIOD);
        }
    }

    private void validateRecruitBeforeActivity(ActivityRequest request) {
        if (request.startAt() != null
                && request.recruitEndAt() != null
                && request.recruitEndAt().isAfter(request.startAt())) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_SCHEDULE_ORDER);
        }
    }

    private void validateUri(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
        }
    }
}
