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
import com.jjikmeok.app.domain.page.dto.response.ActivityCurationDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationSectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePopularActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePopularActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.model.HomeCurationType;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.domain.user.entity.ProfileGender;
import com.jjikmeok.app.domain.user.entity.ProfileStatus;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboarding;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.entity.UserProfile;
import com.jjikmeok.app.domain.onboarding.repository.query.UserOnboardingTagQueryRepository;
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
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
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
    private TagRepository tagRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserOnboardingTagQueryRepository userOnboardingTagRepository;

    private PageServiceImpl pageService;

    @BeforeEach
    void setUp() {
        pageService = new PageServiceImpl(
                activityRepository,
                favoriteRepository,
                imageRepository,
                tagRepository,
                userProfileRepository,
                userOnboardingTagRepository
        );
    }

    @Test
    void getHomePage_buildsSectionedResponse() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile()));
        when(userOnboardingTagRepository.findAllByUserIdWithTag(1L)).thenReturn(onboardingTags());
        List<ActivityRecommendationResponse> recommended = recommendedActivities();
        List<Activity> popular = popularActivities();
        List<Activity> expanded = expandedActivities();
        when(activityRepository.findRecommendedActivitiesByUserTags(
                eq(1L),
                eq(1L),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(recommended);
        when(activityRepository.findApprovedPopularByScore(
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(popular);
        when(favoriteRepository.findActivityIdsByUserIdAndActivityIdIn(eq(1L), anyList()))
                .thenReturn(List.of());
        stubSummaryAssociations(
                Arrays.asList(
                        recommended.get(0).activity(),
                        recommended.get(1).activity(),
                        recommended.get(2).activity(),
                        recommended.get(3).activity(),
                        recommended.get(4).activity(),
                        recommended.get(5).activity(),
                        recommended.get(6).activity(),
                        recommended.get(7).activity(),
                        recommended.get(8).activity(),
                        recommended.get(9).activity(),
                        popular.get(0),
                        popular.get(1),
                        popular.get(2),
                        popular.get(3),
                        popular.get(4),
                        popular.get(5),
                        popular.get(6),
                        popular.get(7),
                        popular.get(8),
                        expanded.get(0),
                        expanded.get(1),
                        expanded.get(2),
                        expanded.get(3),
                        expanded.get(4),
                        expanded.get(5),
                        expanded.get(6),
                        expanded.get(7)
                )
        );

        var response = pageService.getHomePage(1L);

        assertThat(response.user().nickname()).isEqualTo("tester");
        assertThat(response.featured().activities()).hasSize(4);
        assertThat(response.featured().activities().getFirst().title())
                .isEqualTo(HomeCurationType.SOLO_CULTURE.getTitle());
        assertThat(response.featured().activities().getFirst().thumbnailUrl())
                .isEqualTo(HomeCurationType.SOLO_CULTURE.getThumbnailUrl());
        assertThat(response.featured().activities().getFirst().hashtags()).hasSize(2);
        assertThat(response.popular().activities()).hasSize(9);
        assertThat(response.popular().activities().getFirst().thumbnailUrl()).isEqualTo("https://example.com/thumb-11.png");
        assertThat(response.expandedRecommendation().activities()).hasSize(8);
        assertThat(response.expandedRecommendation().activities())
                .allSatisfy(card -> assertThat(card.hashtags()).hasSize(2));
    }

    @Test
    void getCategoryPage_supportsMissingCategoryForProgramType() {
        Activity activity = activity(1L, ActivityCategory.CRAFT, 120, 10);
        when(activityRepository.findApprovedActivityIdsByFiltersNative(isNull(), eq("PROGRAM"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(activityRepository.countApprovedActivitiesByFiltersNative(isNull(), eq("PROGRAM"), any(LocalDateTime.class)))
                .thenReturn(1L);
        stubSummaryAssociations(List.of(activity));

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
        stubSummaryAssociations(List.of(activity));

        ActivityCustomPageResponse response = pageService.getCustomPage(null, 1);

        assertThat(response.recommended().activities()).hasSize(1);
        assertThat(response.recommended().activities().getFirst().hashtags()).hasSize(3);
    }

    @Test
    void getHomeCurationDetailPage_returnsPagedThemeCards() {
        List<Activity> activities = List.of(
                activity(1L, ActivityCategory.CULTURE, 100, 10),
                activity(2L, ActivityCategory.CULTURE, 99, 9),
                activity(3L, ActivityCategory.CULTURE, 98, 8),
                activity(4L, ActivityCategory.CULTURE, 97, 7),
                activity(5L, ActivityCategory.CULTURE, 96, 6),
                activity(6L, ActivityCategory.CULTURE, 95, 5)
        );
        when(tagRepository.findByNameAndType("#감성적", TagType.PREFERENCE_TAG))
                .thenReturn(Optional.of(tag(1L, "#감성적")));
        when(tagRepository.findByNameAndType("#소규모", TagType.PREFERENCE_TAG))
                .thenReturn(Optional.of(tag(2L, "#소규모")));
        when(tagRepository.findByNameAndType("#편안한", TagType.PREFERENCE_TAG))
                .thenReturn(Optional.of(tag(3L, "#편안한")));
        when(tagRepository.findByNameAndType("#휴식", TagType.PREFERENCE_TAG))
                .thenReturn(Optional.of(tag(4L, "#휴식")));
        when(tagRepository.findByNameAndType("#가볍게", TagType.PREFERENCE_TAG))
                .thenReturn(Optional.of(tag(5L, "#가볍게")));
        when(activityRepository.findActiveActivityIdsByTagIds(
                anyList(),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(3);
            List<Long> ids = activities.stream()
                    .map(Activity::getId)
                    .toList();
            int fromIndex = (int) pageable.getOffset();
            int toIndex = Math.min(fromIndex + pageable.getPageSize(), ids.size());
            return ids.subList(fromIndex, toIndex);
        });
        when(activityRepository.countActiveActivityIdsByTagIds(
                anyList(),
                eq(ApprovalStatus.APPROVED),
                any(LocalDateTime.class)
        )).thenReturn((long) activities.size());
        when(favoriteRepository.findActivityIdsByUserIdAndActivityIdIn(eq(1L), anyList()))
                .thenReturn(List.of());
        stubSummaryAssociations(activities);

        var firstPage = pageService.getHomeCurationDetailPage(1L, HomeCurationType.SOLO_CULTURE.getKey(), 0, 5);
        var secondPage = pageService.getHomeCurationDetailPage(1L, HomeCurationType.SOLO_CULTURE.getKey(), 1, 5);

        assertThat(firstPage.title()).isEqualTo(HomeCurationType.SOLO_CULTURE.getTitle());
        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.limit()).isEqualTo(5);
        assertThat(firstPage.activities()).hasSize(5);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextPage()).isEqualTo(1);
        assertThat(firstPage.activities().getFirst().hashtags()).hasSize(2);

        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.limit()).isEqualTo(5);
        assertThat(secondPage.activities()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextPage()).isNull();
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
                UserOnboardingTag.create(onboarding, Tag.create("#편안한", TagType.PREFERENCE_TAG))
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
                .thumbnailUrl("https://example.com/thumb-" + id + ".png")
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
        activity.addTag(Tag.create("#편안한", TagType.PREFERENCE_TAG));
        activity.addTag(Tag.create("#소규모", TagType.PREFERENCE_TAG));
        return activity;
    }

    private ActivityRecommendationResponse recommendation(Activity activity) {
        return new ActivityRecommendationResponse(activity, false);
    }

    private Tag tag(Long id, String name) {
        Tag tag = Tag.create(name, TagType.PREFERENCE_TAG);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    private List<Activity> expandedActivities() {
        return List.of(
                activity(20L, ActivityCategory.SPORTS, 220, 22),
                activity(21L, ActivityCategory.CULTURE, 210, 21),
                activity(22L, ActivityCategory.CRAFT, 200, 20),
                activity(23L, ActivityCategory.COOKING, 190, 19),
                activity(24L, ActivityCategory.PHOTO_VIDEO, 180, 18),
                activity(25L, ActivityCategory.HUMANITIES, 170, 17),
                activity(26L, ActivityCategory.TRAVEL, 160, 16),
                activity(27L, ActivityCategory.LANGUAGE, 150, 15)
        );
    }

    private void stubSummaryAssociations(List<Activity> activities) {
        Map<Long, Activity> activityMap = activities.stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity(), (left, right) -> left));
        when(activityRepository.findAllByIdInWithSummaryAssociations(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(activityMap::get)
                            .filter(Objects::nonNull)
                            .toList();
                });
    }
}
