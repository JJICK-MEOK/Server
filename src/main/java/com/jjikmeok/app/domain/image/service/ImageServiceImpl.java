package com.jjikmeok.app.domain.image.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.image.converter.ImageConverter;
import com.jjikmeok.app.domain.image.dto.request.ImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ImageResponse;
import com.jjikmeok.app.domain.image.entity.Image;
import com.jjikmeok.app.domain.image.repository.ImageRepository;
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
public class ImageServiceImpl implements ImageService {

    private final ActivityRepository activityRepository;
    private final ImageRepository imageRepository;

    @Override
    public List<ImageResponse> getImages(Long activityId) {
        validateActivityExists(activityId);
        return imageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(activityId).stream()
                .map(ImageConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ImageResponse createImage(Long activityId, ImageRequest request) {
        Activity activity = findActivityOrThrow(activityId);
        validateImageUrl(request.imageUrl());
        validateDuplicateSortOrderOnCreate(activityId, request.sortOrder());

        try {
            return ImageConverter.toResponse(
                    imageRepository.save(ImageConverter.toEntity(activity, request))
            );
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        }
    }

    @Override
    @Transactional
    public ImageResponse updateImage(Long activityId, Long imageId, ImageRequest request) {
        validateActivityExists(activityId);
        validateImageUrl(request.imageUrl());

        Image image = findImageOrThrow(activityId, imageId);
        validateDuplicateSortOrderOnUpdate(activityId, imageId, request.sortOrder());

        image.update(request.imageUrl().trim(), request.sortOrder(), request.isThumbnail());
        return ImageConverter.toResponse(image);
    }

    @Override
    @Transactional
    public void deleteImage(Long activityId, Long imageId) {
        validateActivityExists(activityId);
        imageRepository.delete(findImageOrThrow(activityId, imageId));
    }

    private Activity findActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private Image findImageOrThrow(Long activityId, Long imageId) {
        return imageRepository.findByIdAndActivityId(imageId, activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_IMAGE_NOT_FOUND));
    }

    private void validateActivityExists(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }

    private void validateDuplicateSortOrderOnCreate(Long activityId, Integer sortOrder) {
        int normalizedSortOrder = sortOrder != null ? sortOrder : 0;
        if (imageRepository.existsByActivityIdAndSortOrder(activityId, normalizedSortOrder)) {
            throw new CustomException(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        }
    }

    private void validateDuplicateSortOrderOnUpdate(Long activityId, Long imageId, Integer sortOrder) {
        int normalizedSortOrder = sortOrder != null ? sortOrder : 0;
        if (imageRepository.existsByActivityIdAndSortOrderAndIdNot(
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
