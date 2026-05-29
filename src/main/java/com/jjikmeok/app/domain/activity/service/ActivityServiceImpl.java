package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.converter.ActivityConverter;
import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final RegionRepository regionRepository;

    @Override
    public List<ActivitySummaryResponse> getActivities(Long regionId, com.jjikmeok.app.domain.activity.enums.ActivityCategory category, com.jjikmeok.app.domain.activity.enums.ActivityType type, String keyword) {
        if (regionId != null && !regionRepository.existsById(regionId)) {
            throw new CustomException(ErrorCode.REGION_NOT_FOUND);
        }

        return activityRepository.findActiveActivitiesByFilters(regionId, category, type, keyword, LocalDate.now().atStartOfDay()).stream()
                .map(ActivityConverter::toSummaryResponse)
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
        validateActivityRequest(request);

        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));

        Activity activity = ActivityConverter.toEntity(request, region);
        Activity savedActivity = activityRepository.save(activity);

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

        return ActivityConverter.toDetailResponse(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));

        activity.deactivate();
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
