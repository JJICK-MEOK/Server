package com.jjikmeok.app.domain.advertisement.service;

import com.jjikmeok.app.domain.advertisement.dto.request.AdvertisementRequest;
import com.jjikmeok.app.domain.advertisement.dto.response.AdvertisementResponse;
import com.jjikmeok.app.domain.advertisement.entity.Advertisement;
import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import com.jjikmeok.app.domain.advertisement.repository.AdvertisementRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceImplTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 21, 10, 0);

    @Mock
    private AdvertisementRepository advertisementRepository;

    private AdvertisementServiceImpl advertisementService;

    @BeforeEach
    void setUp() {
        advertisementService = new AdvertisementServiceImpl(advertisementRepository);
    }

    @Test
    void getVisibleAdvertisements_returnsVisibleAdvertisementsByPosition() {
        Advertisement advertisement = advertisement(1L, AdvertisementPosition.ACTIVITY_LIST);
        when(advertisementRepository.findVisibleAdvertisements(
                eq(AdvertisementPosition.ACTIVITY_LIST),
                any(LocalDateTime.class)
        )).thenReturn(List.of(advertisement));

        List<AdvertisementResponse> responses =
                advertisementService.getVisibleAdvertisements(AdvertisementPosition.ACTIVITY_LIST);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().position()).isEqualTo(AdvertisementPosition.ACTIVITY_LIST);
    }

    @Test
    void getAdvertisement_returnsVisibleAdvertisement() {
        Advertisement advertisement = advertisement(1L, AdvertisementPosition.ACTIVITY_LIST);
        when(advertisementRepository.findVisibleAdvertisementById(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(advertisement));

        AdvertisementResponse response = advertisementService.getAdvertisement(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getAdvertisement_whenNotFound_throwsAdvertisementNotFound() {
        when(advertisementRepository.findVisibleAdvertisementById(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> advertisementService.getAdvertisement(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADVERTISEMENT_NOT_FOUND);
    }

    @Test
    void createAdvertisement_trimsUrlsAndDefaultsCounters() {
        AdvertisementRequest request = request(" https://example.com/banner.png ", " https://example.com/event ");
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> {
            Advertisement saved = invocation.getArgument(0);
            setId(saved, 1L);
            return saved;
        });

        AdvertisementResponse response = advertisementService.createAdvertisement(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.imageUrl()).isEqualTo("https://example.com/banner.png");
        assertThat(response.redirectUrl()).isEqualTo("https://example.com/event");
        assertThat(response.viewCount()).isZero();
        assertThat(response.clickCount()).isZero();
    }

    @Test
    void createAdvertisement_whenPeriodInvalid_throwsInvalidPeriod() {
        AdvertisementRequest request = new AdvertisementRequest(
                "메인 광고",
                "https://example.com/banner.png",
                "https://example.com/event",
                AdvertisementPosition.MAIN_BANNER,
                0,
                BASE_TIME.plusDays(1),
                BASE_TIME,
                true
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> advertisementService.createAdvertisement(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADVERTISEMENT_INVALID_PERIOD);
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void createAdvertisement_whenUrlInvalid_throwsInvalidUrl() {
        AdvertisementRequest request = request("ftp://example.com/banner.png", "https://example.com/event");

        CustomException exception = assertThrows(CustomException.class,
                () -> advertisementService.createAdvertisement(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADVERTISEMENT_INVALID_URL);
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void createAdvertisement_whenUrlHasNoHost_throwsInvalidUrl() {
        AdvertisementRequest request = request("https:example.com/banner.png", "https://example.com/event");

        CustomException exception = assertThrows(CustomException.class,
                () -> advertisementService.createAdvertisement(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADVERTISEMENT_INVALID_URL);
        verify(advertisementRepository, never()).save(any());
    }

    @Test
    void updateAdvertisement_updatesFields() {
        Advertisement advertisement = advertisement(1L, AdvertisementPosition.ACTIVITY_LIST);
        AdvertisementRequest request = new AdvertisementRequest(
                "수정 광고",
                "https://example.com/new-banner.png",
                "https://example.com/new-event",
                AdvertisementPosition.MAIN_BANNER,
                2,
                BASE_TIME,
                BASE_TIME.plusDays(7),
                false
        );
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(advertisement));

        AdvertisementResponse response = advertisementService.updateAdvertisement(1L, request);

        assertThat(response.title()).isEqualTo("수정 광고");
        assertThat(response.position()).isEqualTo(AdvertisementPosition.MAIN_BANNER);
        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void deleteAdvertisement_deactivatesAdvertisement() {
        Advertisement advertisement = advertisement(1L, AdvertisementPosition.ACTIVITY_LIST);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(advertisement));

        advertisementService.deleteAdvertisement(1L);

        assertThat(advertisement.getIsActive()).isFalse();
    }

    private AdvertisementRequest request(String imageUrl, String redirectUrl) {
        return new AdvertisementRequest(
                "메인 광고",
                imageUrl,
                redirectUrl,
                AdvertisementPosition.ACTIVITY_LIST,
                0,
                BASE_TIME,
                BASE_TIME.plusDays(7),
                true
        );
    }

    private Advertisement advertisement(Long id, AdvertisementPosition position) {
        Advertisement advertisement = Advertisement.builder()
                .title("광고")
                .imageUrl("https://example.com/banner.png")
                .redirectUrl("https://example.com/event")
                .position(position)
                .sortOrder(0)
                .startAt(BASE_TIME)
                .endAt(BASE_TIME.plusDays(7))
                .isActive(true)
                .build();
        setId(advertisement, id);
        return advertisement;
    }

    private void setId(Advertisement advertisement, Long id) {
        ReflectionTestUtils.setField(advertisement, "id", id);
    }
}
