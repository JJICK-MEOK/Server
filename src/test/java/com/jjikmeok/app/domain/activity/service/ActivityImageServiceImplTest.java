package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityImageRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityImageResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityImage;
import com.jjikmeok.app.domain.activity.enums.AgeRange;
import com.jjikmeok.app.domain.activity.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityImageServiceImplTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 21, 10, 0);

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityImageRepository activityImageRepository;

    private ActivityImageServiceImpl activityImageService;

    @BeforeEach
    void setUp() {
        activityImageService = new ActivityImageServiceImpl(activityRepository, activityImageRepository);
    }

    @Test
    void getActivityImages_returnsImagesInSortOrder() {
        Activity activity = activity(1L);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(activityImageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(
                image(10L, activity, 0),
                image(11L, activity, 1)
        ));

        List<ActivityImageResponse> responses = activityImageService.getActivityImages(1L);

        assertThat(responses).extracting(ActivityImageResponse::id).containsExactly(10L, 11L);
    }

    @Test
    void getActivityImages_whenActivityNotFound_throwsActivityNotFound() {
        when(activityRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class,
                () -> activityImageService.getActivityImages(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        verify(activityImageRepository, never()).findAllByActivityIdOrderBySortOrderAscIdAsc(1L);
    }

    @Test
    void createActivityImage_trimsImageUrl() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityImageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(false);
        when(activityImageRepository.save(any(ActivityImage.class))).thenAnswer(invocation -> {
            ActivityImage saved = invocation.getArgument(0);
            setId(saved, 10L);
            return saved;
        });

        ActivityImageResponse response = activityImageService.createActivityImage(
                1L,
                new ActivityImageRequest(" https://example.com/image.png ", 0, true)
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(response.isThumbnail()).isTrue();
    }

    @Test
    void createActivityImage_whenSortOrderDuplicate_throwsConflict() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityImageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> activityImageService.createActivityImage(
                        1L,
                        new ActivityImageRequest("https://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        verify(activityImageRepository, never()).save(any());
    }

    @Test
    void createActivityImage_whenSaveConflicts_throwsDuplicateSortOrder() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityImageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(false);
        when(activityImageRepository.save(any(ActivityImage.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        CustomException exception = assertThrows(CustomException.class,
                () -> activityImageService.createActivityImage(
                        1L,
                        new ActivityImageRequest("https://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
    }

    @Test
    void createActivityImage_whenUrlInvalid_throwsInvalidUrl() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        CustomException exception = assertThrows(CustomException.class,
                () -> activityImageService.createActivityImage(
                        1L,
                        new ActivityImageRequest("ftp://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_INVALID_URL);
    }

    @Test
    void updateActivityImage_updatesFields() {
        Activity activity = activity(1L);
        ActivityImage image = image(10L, activity, 0);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(activityImageRepository.findByIdAndActivityId(10L, 1L)).thenReturn(Optional.of(image));
        when(activityImageRepository.existsByActivityIdAndSortOrderAndIdNot(1L, 2, 10L)).thenReturn(false);

        ActivityImageResponse response = activityImageService.updateActivityImage(
                1L,
                10L,
                new ActivityImageRequest("https://example.com/new.png", 2, true)
        );

        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/new.png");
        assertThat(response.isThumbnail()).isTrue();
    }

    @Test
    void deleteActivityImage_deletesImageFromActivity() {
        Activity activity = activity(1L);
        ActivityImage image = image(10L, activity, 0);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(activityImageRepository.findByIdAndActivityId(10L, 1L)).thenReturn(Optional.of(image));

        activityImageService.deleteActivityImage(1L, 10L);

        verify(activityImageRepository).delete(image);
    }

    private Activity activity(Long id) {
        Region region = Region.builder()
                .name("서울")
                .depth(RegionDepth.PROVINCE)
                .build();
        setId(region, 100L);

        Activity activity = Activity.builder()
                .region(region)
                .title("활동")
                .thumbnailUrl("https://example.com/thumb.png")
                .uri("https://example.com/activity")
                .location("장소")
                .recruitStartAt(BASE_TIME)
                .recruitEndAt(BASE_TIME.plusDays(1))
                .activityStartAt(BASE_TIME.plusDays(2))
                .activityEndAt(BASE_TIME.plusDays(3))
                .ageRange(AgeRange.ANYONE)
                .price(0)
                .description("설명")
                .isActive(true)
                .build();
        setId(activity, id);
        return activity;
    }

    private ActivityImage image(Long id, Activity activity, Integer sortOrder) {
        ActivityImage image = ActivityImage.create(
                activity,
                "https://example.com/image-" + id + ".png",
                sortOrder,
                false
        );
        setId(image, id);
        return image;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
