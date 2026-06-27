package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityFavoriteRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.image.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityFavoriteRepository activityFavoriteRepository;

    @Mock
    private ActivityImageRepository activityImageRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserOnboardingTagRepository userOnboardingTagRepository;

    @Mock
    private UserOnboardingRegionRepository userOnboardingRegionRepository;

    private PageServiceImpl pageService;

    @BeforeEach
    void setUp() {
        pageService = new PageServiceImpl(
                activityRepository,
                activityFavoriteRepository,
                activityImageRepository,
                userProfileRepository,
                userOnboardingTagRepository,
                userOnboardingRegionRepository
        );
    }

    @Test
    void getHomePage_marksTopViewedCardAsAd() {
        Activity activity = activity(1L, 120);
        when(activityRepository.findApprovedLatest(eq(ApprovalStatus.APPROVED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(activity));
        when(activityRepository.findApprovedClosingSoon(eq(ApprovalStatus.APPROVED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        var response = pageService.getHomePage(null, 1);

        assertThat(response.recommendedActivities()).hasSize(1);
        assertThat(response.recommendedActivities().getFirst().isAd()).isTrue();
    }

    private Activity activity(Long id, int viewCount) {
        Region region = Region.builder()
                .name("서울")
                .depth(RegionDepth.PROVINCE)
                .build();
        ReflectionTestUtils.setField(region, "id", 10L);

        Activity activity = Activity.builder()
                .region(region)
                .title("테스트 활동")
                .description("상세 설명")
                .sourceUrl("https://example.com/apply")
                .recruitStartAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .recruitEndAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .startAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .activityType(ActivityType.PROGRAM)
                .category(ActivityCategory.CRAFT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(0)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(activity, "id", id);
        ReflectionTestUtils.setField(activity, "viewCount", viewCount);
        return activity;
    }
}
