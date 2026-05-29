package com.jjikmeok.app.domain.image.service;

import com.jjikmeok.app.domain.image.converter.ActivityImageConverter;
import com.jjikmeok.app.domain.image.dto.request.ActivityImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ActivityImageResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.image.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityImageServiceImpl implements ActivityImageService {

    private final ActivityRepository activityRepository;
    private final ActivityImageRepository activityImageRepository;

    @Override
    public List<ActivityImageResponse> getActivityImages(Long activityId) {
        validateActivityExists(activityId);
        return activityImageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(activityId).stream()
                .map(ActivityImageConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivityImageResponse createActivityImage(Long activityId, ActivityImageRequest request) {
        Activity activity = findActivityOrThrow(activityId);
        validateImageUrl(request.imageUrl());
        validateDuplicateSortOrderOnCreate(activityId, request.sortOrder());

        try {
            return ActivityImageConverter.toResponse(
                    activityImageRepository.save(ActivityImageConverter.toEntity(activity, request))
            );
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        }
    }

    @Override
    @Transactional
    public ActivityImageResponse updateActivityImage(Long activityId, Long imageId, ActivityImageRequest request) {
        validateActivityExists(activityId);
        validateImageUrl(request.imageUrl());

        ActivityImage activityImage = findActivityImageOrThrow(activityId, imageId);
        validateDuplicateSortOrderOnUpdate(activityId, imageId, request.sortOrder());

        activityImage.update(request.imageUrl().trim(), request.sortOrder(), request.isThumbnail());
        return ActivityImageConverter.toResponse(activityImage);
    }

    @Override
    @Transactional
    public void deleteActivityImage(Long activityId, Long imageId) {
        validateActivityExists(activityId);
        activityImageRepository.delete(findActivityImageOrThrow(activityId, imageId));
    }

    private Activity findActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private ActivityImage findActivityImageOrThrow(Long activityId, Long imageId) {
        return activityImageRepository.findByIdAndActivityId(imageId, activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_IMAGE_NOT_FOUND));
    }

    private void validateActivityExists(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    private void validateDuplicateSortOrderOnCreate(Long activityId, Integer sortOrder) {
        int normalizedSortOrder = sortOrder != null ? sortOrder : 0;
        if (activityImageRepository.existsByActivityIdAndSortOrder(activityId, normalizedSortOrder)) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        }
    }

    private void validateDuplicateSortOrderOnUpdate(Long activityId, Long imageId, Integer sortOrder) {
        int normalizedSortOrder = sortOrder != null ? sortOrder : 0;
        if (activityImageRepository.existsByActivityIdAndSortOrderAndIdNot(
                activityId,
                normalizedSortOrder,
                imageId
        )) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        }
    }

    private void validateImageUrl(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_INVALID_URL);
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new CustomException(ErrorCode.ACTIVITY_IMAGE_INVALID_URL);
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_INVALID_URL);
        }
    }
}
