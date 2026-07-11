package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.activity.dto.response.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.favorite.entity.Favorite;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.image.entity.Image;
import com.jjikmeok.app.domain.image.repository.ImageRepository;
import com.jjikmeok.app.domain.page.converter.PageConverter;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCurationDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomeCurationSectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFavoritePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.page.model.HomeCurationType;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.onboarding.repository.query.UserOnboardingTagQueryRepository;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageServiceImpl implements PageService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_CUSTOM_LIMIT = 10;
    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int SORT_FETCH_LIMIT = 100;
    private static final int HOME_CURATION_LIMIT = 4;
    private static final int HOME_POPULAR_LIMIT = 9;
    private static final int HOME_EXPANDED_LIMIT = 8;
    private static final int HOME_CURATION_DETAIL_LIMIT = 4;
    private static final int HOME_RECOMMENDATION_FETCH_LIMIT = 50;
    private static final ApprovalStatus PUBLIC_STATUS = ApprovalStatus.APPROVED;

    private final ActivityRepository activityRepository;
    private final FavoriteRepository favoriteRepository;
    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserOnboardingTagQueryRepository userOnboardingTagQueryRepository;

    @Override
    public ActivityHomePageResponse getHomePage(Long userId) {
        List<UserOnboardingTag> onboardingTags = onboardingTags(userId);
        List<HomeCurationType> featuredCurations = featuredCurations(onboardingTags, HOME_CURATION_LIMIT);

        return new ActivityHomePageResponse(
                homeUser(userId),
                new ActivityHomeCurationSectionResponse(
                        featuredCurationCards(userId, featuredCurations)
                ),
                new ActivityHomeActivitySectionResponse(
                        homeActivityCards(userId, popularActivities(HOME_POPULAR_LIMIT), HOME_POPULAR_LIMIT, 2)
                ),
                new ActivityHomeActivitySectionResponse(
                        homeActivityCards(userId, expandedRecommendationActivities(userId, onboardingTags), HOME_EXPANDED_LIMIT, 2)
                )
        );
    }

    @Override
    public ActivityCategoryPageResponse getCategoryPage(
            Long userId,
            ActivityType type,
            ActivityCategory category,
            String sort,
            Integer limit
    ) {
        int size = limit(limit, DEFAULT_LIST_LIMIT);
        String selectedSort = normalizeSort(sort);
        LocalDateTime now = LocalDate.now(SEOUL).atStartOfDay();
        List<Long> activityIds = activityRepository.findApprovedActivityIdsByFiltersNative(
                category == null ? null : category.name(),
                type == null ? null : type.name(),
                now,
                PageRequest.of(0, Math.max(size, SORT_FETCH_LIMIT))
        );
        List<Activity> activities = activityIds.isEmpty()
                ? List.of()
                : activityRepository.findAllByIdInWithSummaryAssociations(activityIds);

        List<Activity> sorted = sort(activities, selectedSort).stream()
                .limit(size)
                .toList();
        long totalCount = activityRepository.countApprovedActivitiesByFiltersNative(
                category == null ? null : category.name(),
                type == null ? null : type.name(),
                now
        );

        return new ActivityCategoryPageResponse(
                type == null ? "전체" : type.getLabel(),
                type,
                category,
                selectedSort,
                totalCount,
                typeOptions(type),
                categoryOptions(category),
                sortOptions(selectedSort),
                cards(userId, sorted, size)
        );
    }

    @Override
    public ActivityCustomPageResponse getCustomPage(Long userId, Integer limit) {
        int size = limit(limit, DEFAULT_CUSTOM_LIMIT);
        List<UserOnboardingTag> onboardingTags = onboardingTags(userId);
        String nickname = nickname(userId);
        List<String> hashtags = preferenceTags(onboardingTags).stream()
                .map(userOnboardingTag -> "#" + userOnboardingTag.getTag().getName())
                .toList();

        List<ActivityCardResponse> recommended = cards(
                userId,
                personalizedActivities(userId, size, onboardingTags),
                size,
                3
        );

        return new ActivityCustomPageResponse(
                nickname,
                new ActivityCustomPageResponse.TasteProfile(
                        tasteTitle(hashtags),
                        nickname + "님의 취향에 맞는 활동을 모아봤어요.",
                        hashtags
                ),
                new ActivitySectionResponse(recommended)
        );
    }

    @Override
    public ActivityFavoritePageResponse getFavoritePage(Long userId, String sort) {
        String selectedSort = normalizeFavoriteSort(sort);
        LocalDateTime now = LocalDate.now(SEOUL).atStartOfDay();
        List<Activity> activities = favoritePageActivities(userId, selectedSort, now);

        return new ActivityFavoritePageResponse(cards(userId, activities, activities.size(), 2));
    }

    @Override
    @Transactional
    public ActivityDetailPageResponse getDetailPage(Long userId, Long activityId) {
        LocalDateTime recruitCutoff = LocalDate.now(SEOUL).atStartOfDay();
        int updatedCount = activityRepository.incrementApprovedViewCount(activityId, PUBLIC_STATUS, recruitCutoff);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        Activity activity = activityRepository.findApprovedByIdWithRegion(activityId, PUBLIC_STATUS, recruitCutoff)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        activity = enrichActivities(List.of(activity)).getFirst();
        List<Image> images = imageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(activityId);
        boolean liked = userId != null && favoriteRepository.existsByUserIdAndActivityId(userId, activityId);

        return PageConverter.toDetail(activity, images, liked, LocalDate.now(SEOUL));
    }

    @Override
    public ActivityCurationDetailPageResponse getHomeCurationDetailPage(Long userId, String curationKey) {
        HomeCurationType curationType = HomeCurationType.fromKey(curationKey);
        if (curationType == null) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        List<ActivityHomeActivityCardResponse> activities = curationActivities(userId, curationType);
        return new ActivityCurationDetailPageResponse(
                curationType.getTitle(),
                curationType.getSubtitle(),
                curationType.getDisplayHashtags(),
                activities
        );
    }

    private ActivitySectionResponse section(
            Long userId,
            List<Activity> activities,
            int limit,
            int hashtagLimit
    ) {
        return new ActivitySectionResponse(cards(userId, activities, limit, hashtagLimit));
    }

    private List<ActivityHomeActivityCardResponse> homeActivityCards(
            Long userId,
            List<Activity> activities,
            int limit,
            int hashtagLimit
    ) {
        List<Activity> enrichedActivities = enrichActivities(activities);
        List<Activity> distinctActivities = distinct(enrichedActivities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> PageConverter.toHomeActivityCard(
                        activity,
                        likedActivityIds.contains(activity.getId()),
                        today,
                        hashtagLimit
                ))
                .toList();
    }

    private List<ActivityHomeCurationCardResponse> featuredCurationCards(
            Long userId,
            List<HomeCurationType> featuredCurations
    ) {
        return featuredCurations.stream()
                .map(curationType -> {
                    List<ActivityHomeActivityCardResponse> activities = curationActivities(userId, curationType);
                    String thumbnailUrl = activities.stream()
                            .findFirst()
                            .map(ActivityHomeActivityCardResponse::thumbnailUrl)
                            .orElse(null);
                    if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
                        thumbnailUrl = curationType.getThumbnailUrl();
                    }
                    return new ActivityHomeCurationCardResponse(
                            curationType.getTitle(),
                            thumbnailUrl,
                            curationType.getDisplayHashtags()
                    );
                })
                .toList();
    }

    private List<Activity> personalizedActivities(
            Long userId,
            int limit,
            List<UserOnboardingTag> onboardingTags
    ) {
        if (userId == null) {
            return approvedLatestActivities(limit);
        }

        List<UserOnboardingTag> preferenceTags = preferenceTags(onboardingTags);
        if (preferenceTags.isEmpty()) {
            return approvedLatestActivities(limit);
        }

        List<Activity> rankedActivities = activityRepository.findRecommendedActivitiesByUserTags(
                        userId,
                        1L,
                        PUBLIC_STATUS,
                        LocalDateTime.now(SEOUL),
                        PageRequest.of(0, HOME_RECOMMENDATION_FETCH_LIMIT)
                )
                .stream()
                .map(ActivityRecommendationResponse::activity)
                .toList();

        return distinct(rankedActivities).stream()
                .limit(limit)
                .toList();
    }

    private List<Activity> expandedRecommendationActivities(Long userId, List<UserOnboardingTag> onboardingTags) {
        List<UserOnboardingTag> preferenceTags = preferenceTags(onboardingTags);
        if (userId == null || preferenceTags.isEmpty()) {
            return approvedLatestActivities(HOME_EXPANDED_LIMIT);
        }

        Set<ActivityCategory> excludedCategories = selectedTopicCategories(onboardingTags);
        List<Activity> rankedActivities = activityRepository.findRecommendedActivitiesByUserTags(
                        userId,
                        1L,
                        PUBLIC_STATUS,
                        LocalDateTime.now(SEOUL),
                        PageRequest.of(0, HOME_RECOMMENDATION_FETCH_LIMIT)
                )
                .stream()
                .map(ActivityRecommendationResponse::activity)
                .filter(activity -> !excludedCategories.contains(activity.getCategory()))
                .toList();

        return distinct(rankedActivities).stream()
                .limit(HOME_EXPANDED_LIMIT)
                .toList();
    }

    private List<Activity> popularActivities(int limit) {
        return activityRepository.findApprovedPopularByScore(
                PUBLIC_STATUS,
                LocalDateTime.now(SEOUL),
                PageRequest.of(0, limit)
        );
    }

    private List<Activity> approvedLatestActivities(int limit) {
        return activityRepository.findApprovedLatest(
                PUBLIC_STATUS,
                LocalDateTime.now(SEOUL),
                PageRequest.of(0, limit)
        );
    }

    private List<Activity> favoritePageActivities(Long userId, String sort, LocalDateTime now) {
        List<Favorite> favorites = "deadline".equals(sort)
                ? favoriteRepository.findPageFavoritesOrderByDeadlineAsc(userId, PUBLIC_STATUS, now)
                : favoriteRepository.findPageFavoritesOrderBySavedDesc(userId, PUBLIC_STATUS, now);

        return favorites.stream()
                .map(Favorite::getActivity)
                .toList();
    }

    private List<ActivityCardResponse> cards(Long userId, List<Activity> activities, int limit) {
        return cards(userId, activities, limit, 2);
    }

    private List<ActivityCardResponse> cards(Long userId, List<Activity> activities, int limit, int hashtagLimit) {
        List<Activity> enrichedActivities = enrichActivities(activities);
        List<Activity> distinctActivities = distinct(enrichedActivities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> PageConverter.toCard(
                        activity,
                        likedActivityIds.contains(activity.getId()),
                        false,
                        today,
                        hashtagLimit
                ))
                .toList();
    }

    private List<HomeCurationType> featuredCurations(List<UserOnboardingTag> onboardingTags, int limit) {
        Set<String> preferenceTagNames = preferenceTags(onboardingTags).stream()
                .map(userOnboardingTag -> userOnboardingTag.getTag().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Arrays.stream(HomeCurationType.values())
                .sorted(Comparator
                        .comparingInt((HomeCurationType curationType) -> -scoreCuration(curationType, preferenceTagNames))
                        .thenComparingInt(Enum::ordinal))
                .limit(limit)
                .toList();
    }

    private int scoreCuration(HomeCurationType curationType, Set<String> preferenceTagNames) {
        if (preferenceTagNames.isEmpty()) {
            return 0;
        }

        return (int) curationType.getMatchTagNames().stream()
                .filter(preferenceTagNames::contains)
                .count();
    }

    private List<ActivityHomeActivityCardResponse> curationActivities(Long userId, HomeCurationType curationType) {
        List<Long> tagIds = resolveCurationTagIds(curationType);
        if (tagIds.isEmpty()) {
            return List.of();
        }

        List<Activity> activities = activityRepository.findActiveActivitiesByTagIds(
                tagIds,
                PUBLIC_STATUS,
                LocalDateTime.now(SEOUL)
        );
        return homeActivityCards(userId, activities, HOME_CURATION_DETAIL_LIMIT, 2);
    }

    private List<Long> resolveCurationTagIds(HomeCurationType curationType) {
        return curationType.getMatchTagNames().stream()
                .map(tagName -> tagRepository.findByNameAndType(tagName, TagType.PREFERENCE_TAG)
                        .map(Tag::getId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Activity> enrichActivities(List<Activity> activities) {
        List<Long> activityIds = distinct(activities).stream()
                .map(Activity::getId)
                .toList();
        if (activityIds.isEmpty()) {
            return List.of();
        }

        List<Activity> enrichedActivities = activityRepository.findAllByIdInWithSummaryAssociations(activityIds);
        return orderActivitiesByIds(enrichedActivities, activityIds);
    }

    private List<Activity> orderActivitiesByIds(List<Activity> activities, List<Long> activityIds) {
        Map<Long, Integer> orderByActivityId = new HashMap<>();
        for (int i = 0; i < activityIds.size(); i++) {
            orderByActivityId.put(activityIds.get(i), i);
        }

        return activities.stream()
                .sorted(Comparator.comparing(activity -> orderByActivityId.getOrDefault(activity.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private Set<Long> likedActivityIds(Long userId, List<Activity> activities) {
        if (userId == null || activities.isEmpty()) {
            return Set.of();
        }

        List<Long> activityIds = activities.stream()
                .map(Activity::getId)
                .toList();

        return Set.copyOf(favoriteRepository.findActivityIdsByUserIdAndActivityIdIn(userId, activityIds));
    }

    private List<Activity> distinct(List<Activity> activities) {
        Set<Long> seen = new LinkedHashSet<>();
        return activities.stream()
                .filter(activity -> seen.add(activity.getId()))
                .toList();
    }

    private List<Activity> sort(List<Activity> activities, String sort) {
        Comparator<Activity> comparator = switch (sort) {
            case "deadline" -> Comparator
                    .comparing(Activity::getRecruitEndAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Activity::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()));
            case "popular" -> Comparator
                    .comparing(Activity::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Activity::getLikeCount, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Activity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Activity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };

        return distinct(activities).stream()
                .sorted(comparator)
                .toList();
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "recommended";
        }

        String value = sort.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "deadline", "popular" -> value;
            default -> "recommended";
        };
    }

    private String normalizeFavoriteSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "saved";
        }

        String value = sort.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "deadline" -> "deadline";
            default -> "saved";
        };
    }

    private int limit(Integer requested, int defaultValue) {
        if (requested == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(requested, MAX_LIMIT));
    }

    private String nickname(Long userId) {
        if (userId == null) {
            return "게스트";
        }
        return userProfileRepository.findByUserId(userId)
                .map(userProfile -> userProfile.getNickname())
                .orElse("게스트");
    }

    private ActivityHomePageResponse.UserResponse homeUser(Long userId) {
        if (userId == null) {
            return new ActivityHomePageResponse.UserResponse("게스트", "");
        }

        return userProfileRepository.findByUserId(userId)
                .map(userProfile -> new ActivityHomePageResponse.UserResponse(
                        userProfile.getNickname(),
                        userProfile.getProfileImageUrl()
                ))
                .orElseGet(() -> new ActivityHomePageResponse.UserResponse("게스트", ""));
    }

    private List<UserOnboardingTag> onboardingTags(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userOnboardingTagQueryRepository.findAllByUserIdWithTag(userId);
    }

    private List<UserOnboardingTag> preferenceTags(List<UserOnboardingTag> onboardingTags) {
        return onboardingTags.stream()
                .filter(userOnboardingTag -> userOnboardingTag.getTag().getType() == TagType.PREFERENCE_TAG)
                .toList();
    }

    private Set<ActivityCategory> selectedTopicCategories(List<UserOnboardingTag> onboardingTags) {
        return onboardingTags.stream()
                .filter(userOnboardingTag -> userOnboardingTag.getTag().getType() == TagType.TOPIC_CATEGORY)
                .map(userOnboardingTag -> activityCategory(userOnboardingTag.getTag().getName()))
                .filter(category -> category != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ActivityCategory activityCategory(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }

        String normalizedLabel = label.replace(" ", "");
        for (ActivityCategory category : ActivityCategory.values()) {
            if (category.getLabel().replace(" ", "").equals(normalizedLabel)) {
                return category;
            }
        }
        return null;
    }

    private String tasteTitle(List<String> hashtags) {
        if (hashtags.isEmpty()) {
            return "아직 선호 정보가 부족해요";
        }
        if (hashtags.stream().anyMatch(tag -> tag.contains("운동") || tag.contains("액티비티") || tag.contains("등산"))) {
            return "활동적인 취향이네요";
        }
        if (hashtags.stream().anyMatch(tag -> tag.contains("모임") || tag.contains("스터디") || tag.contains("클럽"))) {
            return "함께하는 활동을 좋아하시네요";
        }
        if (hashtags.stream().anyMatch(tag -> tag.contains("클래스") || tag.contains("교육") || tag.contains("강연"))) {
            return "배움이 있는 활동을 선호하시네요";
        }
        return "아직 선호 정보가 부족해요";
    }

    private List<ActivityFilterOptionResponse> typeOptions(ActivityType selectedType) {
        List<ActivityFilterOptionResponse> options = new java.util.ArrayList<>();
        options.add(new ActivityFilterOptionResponse("", "전체", selectedType == null));
        for (ActivityType type : ActivityType.values()) {
            options.add(new ActivityFilterOptionResponse(type.name(), type.getLabel(), type == selectedType));
        }
        return options;
    }

    private List<ActivityFilterOptionResponse> categoryOptions(ActivityCategory selectedCategory) {
        List<ActivityFilterOptionResponse> options = new java.util.ArrayList<>();
        options.add(new ActivityFilterOptionResponse("", "전체", selectedCategory == null));
        for (ActivityCategory category : ActivityCategory.values()) {
            options.add(new ActivityFilterOptionResponse(category.name(), category.getLabel(), category == selectedCategory));
        }
        return options;
    }

    private List<ActivityFilterOptionResponse> sortOptions(String selectedSort) {
        return List.of(
                new ActivityFilterOptionResponse("recommended", "추천순", "recommended".equals(selectedSort)),
                new ActivityFilterOptionResponse("popular", "인기순", "popular".equals(selectedSort)),
                new ActivityFilterOptionResponse("deadline", "마감순", "deadline".equals(selectedSort))
        );
    }
}
