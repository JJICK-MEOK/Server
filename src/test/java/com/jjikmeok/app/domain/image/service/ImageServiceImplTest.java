package com.jjikmeok.app.domain.image.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.image.dto.request.ImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ImageResponse;
import com.jjikmeok.app.domain.image.entity.Image;
import com.jjikmeok.app.domain.image.repository.ImageRepository;
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
class ImageServiceImplTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 21, 10, 0);

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ImageRepository imageRepository;

    private ImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageServiceImpl(activityRepository, imageRepository);
    }

    @Test
    void getImages_returnsImagesInSortOrder() {
        Activity activity = activity(1L);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(imageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(
                image(10L, activity, 0),
                image(11L, activity, 1)
        ));

        List<ImageResponse> responses = imageService.getImages(1L);

        assertThat(responses).extracting(ImageResponse::id).containsExactly(10L, 11L);
    }

    @Test
    void getImages_whenActivityNotFound_throwsActivityNotFound() {
        when(activityRepository.existsById(1L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class,
                () -> imageService.getImages(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        verify(imageRepository, never()).findAllByActivityIdOrderBySortOrderAscIdAsc(1L);
    }

    @Test
    void createImage_trimsImageUrl() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(imageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(false);
        when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> {
            Image saved = invocation.getArgument(0);
            setId(saved, 10L);
            return saved;
        });

        ImageResponse response = imageService.createImage(
                1L,
                new ImageRequest(" https://example.com/image.png ", 0, true)
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(response.isThumbnail()).isTrue();
    }

    @Test
    void createImage_whenSortOrderDuplicate_throwsConflict() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(imageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> imageService.createImage(
                        1L,
                        new ImageRequest("https://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
        verify(imageRepository, never()).save(any());
    }

    @Test
    void createImage_whenSaveConflicts_throwsDuplicateSortOrder() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(imageRepository.existsByActivityIdAndSortOrder(1L, 0)).thenReturn(false);
        when(imageRepository.save(any(Image.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        CustomException exception = assertThrows(CustomException.class,
                () -> imageService.createImage(
                        1L,
                        new ImageRequest("https://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_DUPLICATE_SORT_ORDER);
    }

    @Test
    void createImage_whenUrlInvalid_throwsInvalidUrl() {
        Activity activity = activity(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        CustomException exception = assertThrows(CustomException.class,
                () -> imageService.createImage(
                        1L,
                        new ImageRequest("ftp://example.com/image.png", 0, false)
                ));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_IMAGE_INVALID_URL);
    }

    @Test
    void updateImage_updatesFields() {
        Activity activity = activity(1L);
        Image image = image(10L, activity, 0);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(imageRepository.findByIdAndActivityId(10L, 1L)).thenReturn(Optional.of(image));
        when(imageRepository.existsByActivityIdAndSortOrderAndIdNot(1L, 2, 10L)).thenReturn(false);

        ImageResponse response = imageService.updateImage(
                1L,
                10L,
                new ImageRequest("https://example.com/new.png", 2, true)
        );

        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/new.png");
        assertThat(response.isThumbnail()).isTrue();
    }

    @Test
    void deleteImage_deletesImageFromActivity() {
        Activity activity = activity(1L);
        Image image = image(10L, activity, 0);
        when(activityRepository.existsById(1L)).thenReturn(true);
        when(imageRepository.findByIdAndActivityId(10L, 1L)).thenReturn(Optional.of(image));

        imageService.deleteImage(1L, 10L);

        verify(imageRepository).delete(image);
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
                .description("설명")
                .thumbnailUrl("https://example.com/thumb.png")
                .sourceUrl("https://example.com/activity")
                .address("주소")
                .recruitStartAt(BASE_TIME)
                .recruitEndAt(BASE_TIME.plusDays(1))
                .startAt(BASE_TIME.plusDays(2))
                .endAt(BASE_TIME.plusDays(3))
                .price(0)
                .isActive(true)
                .build();
        setId(activity, id);
        return activity;
    }

    private Image image(Long id, Activity activity, Integer sortOrder) {
        Image image = Image.create(
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
