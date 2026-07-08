package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.image.repository.ImageRepository;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.user.entity.ProfileGender;
import com.jjikmeok.app.domain.user.entity.ProfileStatus;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserOnboarding;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.entity.UserProfile;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserOnboardingTagRepository userOnboardingTagRepository;

    private PageServiceImpl pageService;

    @BeforeEach
    void setUp() {
        pageService = new PageServiceImpl(
                activityRepository,
                favoriteRepository,
                imageRepository,
                userProfileRepository,
                userOnboardingTagRepository
        );
    }

    @Test
    void getHomePage_buildsSectionedResponse() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile()));
        when(userOnboardingTagRepository.findAllByUserIdWithTag(1L)).thenReturn(onboardingTags());
        when(activityRepository.findRecommendedActivitiesByUserTags(
                eq(1L),
                eq(1L),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(recommendedActivities());
        when(activityRepository.findApprovedPopularByScore(
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(popularActivities());
        when(favoriteRepository.findActivityIdsByUserIdAndActivityIdIn(eq(1L), anyList()))
                .thenReturn(List.of());

        var response = pageService.getHomePage(1L);

        assertThat(response.user().nickname()).isEqualTo("tester");
        assertThat(response.featured().activities()).hasSize(4);
        assertThat(response.featured().activities().getFirst().isAd()).isFalse();
        assertThat(response.popular().activities()).hasSize(9);
        assertThat(response.expandedRecommendation().activities()).hasSize(8);
        assertThat(response.expandedRecommendation().activities())
                .allSatisfy(card -> assertThat(card.category())
                        .isNotIn(ActivityCategory.SPORTS, ActivityCategory.CULTURE));
    }

    @Test
    void getCategoryPage_supportsMissingCategoryForProgramType() {
        Activity activity = activity(1L, ActivityCategory.CRAFT, 120, 10);
        when(activityRepository.findApprovedActivityIdsByFiltersNative(isNull(), eq("PROGRAM"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(activityRepository.findAllByIdInWithSummaryAssociations(List.of(1L)))
                .thenReturn(List.of(activity));
        when(activityRepository.countApprovedActivitiesByFiltersNative(isNull(), eq("PROGRAM"), any(LocalDateTime.class)))
                .thenReturn(1L);

        ActivityCategoryPageResponse response = pageService.getCategoryPage(null, ActivityType.PROGRAM, null, null, 20);

        assertThat(response.selectedType()).isEqualTo(ActivityType.PROGRAM);
        assertThat(response.selectedCategory()).isNull();
        assertThat(response.activities()).hasSize(1);
    }

    @Test
    void getCustomPage_usesThreeHashtagsPerActivity() {
        Activity activity = activity(1L, ActivityCategory.CRAFT, 120, 10);
        when(activityRepository.findApprovedLatest(eq(ApprovalStatus.APPROVED), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(activity));

        ActivityCustomPageResponse response = pageService.getCustomPage(null, 1);

        assertThat(response.recommended().activities()).hasSize(1);
        assertThat(response.recommended().activities().getFirst().hashtags()).hasSize(3);
    }

    private UserProfile userProfile() {
        User user = User.createForSignup("tester@example.com", "password");
        UserProfile profile = UserProfile.create(
                user,
                "tester",
                LocalDate.of(1990, 1, 1),
                ProfileGender.MALE,
                ProfileStatus.STUDENT,
                true,
                true,
                false
        );
        ReflectionTestUtils.setField(profile, "profileImageUrl", "https://example.com/profile.png");
        return profile;
    }

    private List<UserOnboardingTag> onboardingTags() {
        User user = User.createForSignup("tester@example.com", "password");
        UserOnboarding onboarding = UserOnboarding.create(user);
        return List.of(
                UserOnboardingTag.create(onboarding, Tag.create("운동 / 액티비티", TagType.TOPIC_CATEGORY)),
                UserOnboardingTag.create(onboarding, Tag.create("문화 / 예술", TagType.TOPIC_CATEGORY)),
                UserOnboardingTag.create(onboarding, Tag.create("편안한", TagType.PREFERENCE_TAG))
        );
    }

    private List<ActivityRecommendationResponse> recommendedActivities() {
        return List.of(
                recommendation(activity(1L, ActivityCategory.SPORTS, 120, 20)),
                recommendation(activity(2L, ActivityCategory.CULTURE, 118, 18)),
                recommendation(activity(3L, ActivityCategory.CRAFT, 116, 16)),
                recommendation(activity(4L, ActivityCategory.COOKING, 114, 14)),
                recommendation(activity(5L, ActivityCategory.PHOTO_VIDEO, 112, 12)),
                recommendation(activity(6L, ActivityCategory.HUMANITIES, 110, 10)),
                recommendation(activity(7L, ActivityCategory.TRAVEL, 108, 8)),
                recommendation(activity(8L, ActivityCategory.LANGUAGE, 106, 6)),
                recommendation(activity(9L, ActivityCategory.VOLUNTEER, 104, 4)),
                recommendation(activity(10L, ActivityCategory.CAREER, 102, 2))
        );
    }

    private List<Activity> popularActivities() {
        return List.of(
                activity(11L, ActivityCategory.CAREER, 200, 20),
                activity(12L, ActivityCategory.VOLUNTEER, 190, 19),
                activity(13L, ActivityCategory.LANGUAGE, 180, 18),
                activity(14L, ActivityCategory.TRAVEL, 170, 17),
                activity(15L, ActivityCategory.HUMANITIES, 160, 16),
                activity(16L, ActivityCategory.PHOTO_VIDEO, 150, 15),
                activity(17L, ActivityCategory.COOKING, 140, 14),
                activity(18L, ActivityCategory.CRAFT, 130, 13),
                activity(19L, ActivityCategory.CULTURE, 120, 12)
        );
    }

    private Activity activity(Long id, ActivityCategory category, int viewCount, int likeCount) {
        Activity activity = Activity.builder()
                .region(com.jjikmeok.app.domain.region.entity.Region.builder()
                        .name("서울")
                        .build())
                .title("테스트 활동 " + id)
                .description("상세 설명")
                .sourceUrl("https://example.com/apply/" + id)
                .recruitStartAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .recruitEndAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .startAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .activityType(ActivityType.PROGRAM)
                .category(category)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(0)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(activity, "id", id);
        ReflectionTestUtils.setField(activity, "viewCount", viewCount);
        ReflectionTestUtils.setField(activity, "likeCount", likeCount);
        return activity;
    }

    private ActivityRecommendationResponse recommendation(Activity activity) {
        return new ActivityRecommendationResponse(activity, false);
    }
}
