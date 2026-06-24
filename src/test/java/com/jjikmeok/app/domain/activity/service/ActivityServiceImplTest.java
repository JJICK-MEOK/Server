package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 14, 10, 0);

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private RegionRepository regionRepository;

    private ActivityServiceImpl activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityServiceImpl(activityRepository, regionRepository);
    }

    @Test
    void getActivities_withoutRegionId_returnsActiveActivities() {
        Region region = region(10L, "서울", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        when(activityRepository.findActiveActivitiesByFilters(isNull(), isNull(), isNull(), isNull(), any(LocalDateTime.class))).thenReturn(List.of(activity));

        List<ActivitySummaryResponse> responses = activityService.getActivities(null, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(1L);
        assertThat(responses.getFirst().regionId()).isEqualTo(10L);
        assertThat(responses.getFirst().price()).isEqualTo(1000);
        verify(activityRepository).findActiveActivitiesByFilters(isNull(), isNull(), isNull(), isNull(), any(LocalDateTime.class));
    }

    @Test
    void getActivities_withRegionId_returnsRegionActivities() {
        Region region = region(10L, "서울", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        when(regionRepository.existsById(10L)).thenReturn(true);
        when(activityRepository.findActiveActivitiesByFilters(eq(10L), isNull(), isNull(), isNull(), any(LocalDateTime.class))).thenReturn(List.of(activity));

        List<ActivitySummaryResponse> responses = activityService.getActivities(10L, null, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().regionId()).isEqualTo(10L);
        verify(regionRepository).existsById(10L);
        verify(activityRepository).findActiveActivitiesByFilters(eq(10L), isNull(), isNull(), isNull(), any(LocalDateTime.class));
    }

    @Test
    void getActivities_withUnknownRegionId_throwsRegionNotFound() {
        when(regionRepository.existsById(999L)).thenReturn(false);

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.getActivities(999L, null, null, null));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
        verifyNoInteractions(activityRepository);
    }

    @Test
    void searchActivitiesByTags_returnsMatchedActivities() {
        Region region = region(10L, "Seoul", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        when(activityRepository.findActiveActivitiesByTagIds(
                eq(List.of(1L, 2L)),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(activity));

        List<ActivitySummaryResponse> responses = activityService.searchActivitiesByTags(List.of(1L, 1L, 2L));

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(1L);
        assertThat(responses.getFirst().regionId()).isEqualTo(10L);
        verify(activityRepository).findActiveActivitiesByTagIds(
                eq(List.of(1L, 2L)),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class)
        );
    }

    @Test
    void searchActivitiesByTags_withoutTagIds_returnsEmptyList() {
        List<ActivitySummaryResponse> responses = activityService.searchActivitiesByTags(List.of());

        assertThat(responses).isEmpty();
        verify(activityRepository, never()).findActiveActivitiesByTagIds(any(), any(), any());
    }

    @Test
    void getRecommendedActivities_returnsActivitiesMatchedByUserTags() {
        Region region = region(10L, "Seoul", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        when(activityRepository.findRecommendedActivitiesByUserTags(
                eq(1L),
                eq(3L),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                eq(PageRequest.of(0, 8))
        ))
                .thenReturn(List.of(new ActivityRecommendationResponse(activity, true)));

        List<ActivitySummaryResponse> responses = activityService.getRecommendedActivities(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(1L);
        assertThat(responses.getFirst().regionId()).isEqualTo(10L);
        assertThat(responses.getFirst().liked()).isTrue();
        verify(activityRepository).findRecommendedActivitiesByUserTags(
                eq(1L),
                eq(3L),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                eq(PageRequest.of(0, 8))
        );
    }

    @Test
    void createActivity_allowsFreePriceZero() {
        Region region = region(10L, "서울", RegionDepth.PROVINCE, null);
        ActivityRequest request = activityRequest(0);
        when(regionRepository.findById(10L)).thenReturn(Optional.of(region));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity savedActivity = invocation.getArgument(0);
            setId(savedActivity, 1L);
            return savedActivity;
        });

        ActivityDetailResponse response = activityService.createActivity(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(10L);
        assertThat(response.price()).isZero();

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getPrice()).isZero();
    }

    @Test
    void createActivity_whenRecruitPeriodInvalid_throwsRecruitPeriodError() {
        ActivityRequest request = activityRequest(
                BASE_TIME.plusDays(2),
                BASE_TIME.plusDays(1),
                BASE_TIME.plusDays(3),
                BASE_TIME.plusDays(4),
                "https://example.com/apply"
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.createActivity(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_RECRUIT_PERIOD);
        verifyNoInteractions(regionRepository, activityRepository);
    }

    @Test
    void createActivity_whenActivityPeriodInvalid_throwsActivityPeriodError() {
        ActivityRequest request = activityRequest(
                BASE_TIME,
                BASE_TIME.plusDays(1),
                BASE_TIME.plusDays(4),
                BASE_TIME.plusDays(3),
                "https://example.com/apply"
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.createActivity(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_ACTIVITY_PERIOD);
        verifyNoInteractions(regionRepository, activityRepository);
    }

    @Test
    void createActivity_whenRecruitEndAfterActivityStart_throwsScheduleOrderError() {
        ActivityRequest request = activityRequest(
                BASE_TIME,
                BASE_TIME.plusDays(3),
                BASE_TIME.plusDays(2),
                BASE_TIME.plusDays(4),
                "https://example.com/apply"
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.createActivity(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_SCHEDULE_ORDER);
        verifyNoInteractions(regionRepository, activityRepository);
    }

    @Test
    void createActivity_whenUriInvalid_throwsUriError() {
        ActivityRequest request = activityRequest(
                BASE_TIME,
                BASE_TIME.plusDays(1),
                BASE_TIME.plusDays(2),
                BASE_TIME.plusDays(3),
                "ftp://example.com/apply"
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.createActivity(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_INVALID_URI);
        verifyNoInteractions(regionRepository, activityRepository);
    }

    @Test
    void createActivity_whenRegionNotFound_throwsRegionNotFound() {
        ActivityRequest request = activityRequest(0);
        when(regionRepository.findById(10L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.createActivity(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void getActivity_increasesViewCountAtomically() {
        Region region = region(10L, "Seoul", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        ReflectionTestUtils.setField(activity, "viewCount", 1);
        when(activityRepository.incrementViewCount(eq(1L), any(LocalDateTime.class))).thenReturn(1);
        when(activityRepository.findOpenByIdWithRegion(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(activity));

        ActivityDetailResponse response = activityService.getActivity(1L);

        assertThat(response.viewCount()).isEqualTo(1);
        verify(activityRepository).incrementViewCount(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void getActivity_whenAtomicIncrementTargetNotFound_throwsActivityNotFound() {
        when(activityRepository.incrementViewCount(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.getActivity(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        verify(activityRepository, never()).findOpenByIdWithRegion(eq(1L), any(LocalDateTime.class));
    }
@Test
    void updateActivity_updatesFields() {
        Region oldRegion = region(10L, "서울", RegionDepth.PROVINCE, null);
        Region newRegion = region(20L, "경기", RegionDepth.PROVINCE, null);
        Activity activity = activity(oldRegion);
        setId(activity, 1L);
        ActivityRequest request = activityRequest(0);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(regionRepository.findById(10L)).thenReturn(Optional.of(newRegion));

        ActivityDetailResponse response = activityService.updateActivity(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.regionId()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("테스트 활동");
        assertThat(response.price()).isZero();
        assertThat(activity.getRegion()).isSameAs(newRegion);
    }

    @Test
    void deleteActivity_deactivatesActivity() {
        Region region = region(10L, "서울", RegionDepth.PROVINCE, null);
        Activity activity = activity(region);
        setId(activity, 1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        activityService.deleteActivity(1L);

        assertThat(activity.getIsActive()).isFalse();
    }

    @Test
    void deleteActivity_whenNotFound_throwsActivityNotFound() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> activityService.deleteActivity(1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
    }

    private ActivityRequest activityRequest(Integer price) {
        return activityRequest(
                BASE_TIME,
                BASE_TIME.plusDays(7),
                BASE_TIME.plusDays(8),
                BASE_TIME.plusDays(9),
                "https://example.com/apply",
                price
        );
    }

    private ActivityRequest activityRequest(LocalDateTime recruitStartAt, LocalDateTime recruitEndAt,
                                            LocalDateTime activityStartAt, LocalDateTime activityEndAt,
                                            String sourceUrl) {
        return activityRequest(recruitStartAt, recruitEndAt, activityStartAt, activityEndAt, sourceUrl, 0);
    }

    private ActivityRequest activityRequest(LocalDateTime recruitStartAt, LocalDateTime recruitEndAt,
                                            LocalDateTime activityStartAt, LocalDateTime activityEndAt,
                                            String sourceUrl, Integer price) {
        return new ActivityRequest(
                10L,
                "테스트 활동",
                "활동 상세 설명",
                "https://example.com/thumb.png",
                sourceUrl,
                "서울마포도서관",
                activityStartAt,
                activityEndAt,
                recruitStartAt,
                recruitEndAt,
                price,
                ActivityType.ONE_DAY,
                ActivityCategory.CRAFT,
                SourceType.URL_MANUAL,
                null,
                ApprovalStatus.PENDING,
                true
        );
    }

    private Activity activity(Region region) {
        return Activity.builder()
                .region(region)
                .title("기존 활동")
                .description("기존 상세 설명")
                .thumbnailUrl("https://example.com/old-thumb.png")
                .sourceUrl("https://example.com/old-apply")
                .address("기존 장소")
                .recruitStartAt(BASE_TIME)
                .recruitEndAt(BASE_TIME.plusDays(7))
                .startAt(BASE_TIME.plusDays(8))
                .endAt(BASE_TIME.plusDays(9))
                .activityType(ActivityType.PROGRAM)
                .category(ActivityCategory.SELF_DEVELOPMENT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.PENDING)
                .price(1000)
                .isActive(true)
                .build();
    }

    private Region region(Long id, String name, RegionDepth depth, Region parent) {
        Region region = Region.builder()
                .parent(parent)
                .name(name)
                .depth(depth)
                .build();
        setId(region, id);
        return region;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
