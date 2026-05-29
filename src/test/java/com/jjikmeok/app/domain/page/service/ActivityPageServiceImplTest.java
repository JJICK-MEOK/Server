package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.image.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityPageServiceImplTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private FavoriteRepository activityFavoriteRepository;

    @Mock
    private ActivityImageRepository activityImageRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOnboardingTagRepository userOnboardingTagRepository;

    @Mock
    private UserOnboardingRegionRepository userOnboardingRegionRepository;

    private PageServiceImpl activityPageService;

    @BeforeEach
    void setUp() {
        activityPageService = new PageServiceImpl(
                activityRepository,
                activityFavoriteRepository,
                activityImageRepository,
                userProfileRepository,
                userRepository,
                userOnboardingTagRepository,
                userOnboardingRegionRepository
        );
    }

    @Test
    void getDetailPage_returnsScreenReadyDetail() {
        Activity activity = activity();
        ActivityImage image = ActivityImage.create(activity, "https://example.com/image.png", 0, true);
        setId(image, 5L);

        when(activityRepository.incrementApprovedViewCount(eq(1L), eq(ApprovalStatus.APPROVED), any())).thenReturn(1);
        when(activityRepository.findApprovedByIdWithRegion(eq(1L), eq(ApprovalStatus.APPROVED), any())).thenReturn(Optional.of(activity));
        when(activityImageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(image));
        when(activityFavoriteRepository.existsByUserIdAndActivityId(7L, 1L)).thenReturn(true);

        ActivityDetailPageResponse response = activityPageService.getDetailPage(7L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.organizer()).isEqualTo("운영기관");
        assertThat(response.contactInfo()).isEqualTo("010-0000-0000");
        assertThat(response.target()).isEqualTo("청년");
        assertThat(response.images()).extracting("imageUrl").containsExactly("https://example.com/image.png");
        assertThat(response.liked()).isTrue();
        assertThat(response.dDay()).isEqualTo("D-3");
        assertThat(response.priceLabel()).isEqualTo("무료");
    }

    @Test
    void getDetailPage_whenActivityNotFound_throwsActivityNotFound() {
        when(activityRepository.incrementApprovedViewCount(eq(1L), eq(ApprovalStatus.APPROVED), any())).thenReturn(0);

        CustomException exception = assertThrows(CustomException.class,
                () -> activityPageService.getDetailPage(7L, 1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        verify(activityRepository).incrementApprovedViewCount(eq(1L), eq(ApprovalStatus.APPROVED), any());
    }

    private Activity activity() {
        Region region = Region.builder()
                .name("서울")
                .depth(RegionDepth.PROVINCE)
                .build();
        setId(region, 10L);

        LocalDate today = LocalDate.now(SEOUL);
        Activity activity = Activity.builder()
                .region(region)
                .title("테스트 활동")
                .description("상세 설명")
                .thumbnailUrl("https://example.com/thumb.png")
                .sourceUrl("https://example.com/apply")
                .address("서울")
                .organizer("운영기관")
                .contactInfo("010-0000-0000")
                .target("청년")
                .recruitStartAt(today.atStartOfDay())
                .recruitEndAt(today.plusDays(3).atStartOfDay())
                .startAt(today.plusDays(4).atStartOfDay())
                .endAt(today.plusDays(4).atStartOfDay())
                .price(0)
                .activityType(ActivityType.PROGRAM)
                .category(ActivityCategory.CRAFT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build();
        setId(activity, 1L);
        return activity;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
