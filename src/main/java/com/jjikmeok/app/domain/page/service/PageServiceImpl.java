package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.repository.FavoriteRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.image.entity.Image;
import com.jjikmeok.app.domain.image.repository.ImageRepository;
import com.jjikmeok.app.domain.page.converter.PageConverter;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PageServiceImpl implements PageService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_HOME_LIMIT = 10;
    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int SORT_FETCH_LIMIT = 100;
    private static final ApprovalStatus PUBLIC_STATUS = ApprovalStatus.APPROVED;

    private final ActivityRepository activityRepository;
    private final FavoriteRepository favoriteRepository;
    private final ImageRepository imageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserOnboardingTagRepository userOnboardingTagRepository;
    private final UserOnboardingRegionRepository userOnboardingRegionRepository;

    @Override
    public ActivityHomePageResponse getHomePage(Long userId, Integer limit) {
        int size = limit(limit, DEFAULT_HOME_LIMIT);
        ActivityHomePageResponse.UserResponse user = homeUser(userId);

        List<ActivityCardResponse> recommended = cards(userId, recommendedActivities(userId, size), size);
        List<ActivityCardResponse> closingSoon = cards(
                userId,
                activityRepository.findApprovedClosingSoon(PUBLIC_STATUS, LocalDateTime.now(SEOUL), PageRequest.of(0, size)),
                size
        );

        return new ActivityHomePageResponse(user, recommended, closingSoon);
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
        List<Activity> activities = activityRepository.findApprovedActivitiesByFilters(
                PUBLIC_STATUS,
                category,
                type,
                now,
                PageRequest.of(0, Math.max(size, SORT_FETCH_LIMIT))
        );

        List<Activity> sorted = sort(activities, selectedSort).stream()
                .limit(size)
                .toList();
        long totalCount = activityRepository.countApprovedActivitiesByFilters(PUBLIC_STATUS, category, type, now);

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
        int size = limit(limit, DEFAULT_HOME_LIMIT);
        String nickname = nickname(userId);
        List<UserOnboardingTag> preferenceTags = preferenceTags(userId);
        List<String> hashtags = preferenceTags.stream()
                .map(userOnboardingTag -> "#" + userOnboardingTag.getTag().getName())
                .toList();

        List<ActivityCardResponse> recommended = cards(userId, recommendedActivities(userId, size), size);

        return new ActivityCustomPageResponse(
                nickname,
                new ActivityCustomPageResponse.TasteProfile(
                        tasteTitle(hashtags),
                        nickname + "님의 취향에 맞는 활동을 모아봤어요.",
                        hashtags
                ),
                new ActivitySectionResponse("customRecommended", "맞춤 추천 활동", null, recommended)
        );
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
        List<Image> images = imageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(activityId);
        boolean liked = userId != null && favoriteRepository.existsByUserIdAndActivityId(userId, activityId);

        return PageConverter.toDetail(activity, images, liked, LocalDate.now(SEOUL));
    }

    private List<Activity> recommendedActivities(Long userId, int size) {
        LocalDateTime now = LocalDate.now(SEOUL).atStartOfDay();
        List<Long> tagIds = preferenceTags(userId).stream()
                .map(userOnboardingTag -> userOnboardingTag.getTag().getId())
                .toList();

        if (!tagIds.isEmpty()) {
            List<Activity> byTags = activityRepository.findRecommendedByPreferenceTagIds(
                    PUBLIC_STATUS,
                    tagIds,
                    now,
                    PageRequest.of(0, size)
            );
            if (!byTags.isEmpty()) {
                return byTags;
            }
        }

        List<Long> regionIds = userId == null ? List.of() : userOnboardingRegionRepository.findRegionIdsByUserId(userId);
        if (!regionIds.isEmpty()) {
            List<Activity> byRegions = activityRepository.findRecommendedByRegionIds(
                    PUBLIC_STATUS,
                    regionIds,
                    now,
                    PageRequest.of(0, size)
            );
            if (!byRegions.isEmpty()) {
                return byRegions;
            }
        }

        return activityRepository.findApprovedLatest(PUBLIC_STATUS, now, PageRequest.of(0, size));
    }

    private List<ActivityCardResponse> cards(Long userId, List<Activity> activities, int limit) {
        List<Activity> distinctActivities = distinct(activities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        Set<Long> adActivityIds = adActivityIds(distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> PageConverter.toCard(
                        activity,
                        likedActivityIds.contains(activity.getId()),
                        adActivityIds.contains(activity.getId()),
                        today
                ))
                .toList();
    }

    private Set<Long> adActivityIds(List<Activity> activities) {
        List<Activity> ranked = activities.stream()
                .sorted(Comparator
                        .comparing(Activity::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Activity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .toList();

        if (ranked.isEmpty()) {
            return Set.of();
        }

        List<Activity> shuffled = new java.util.ArrayList<>(ranked);
        java.util.Collections.shuffle(shuffled);
        return Set.of(shuffled.getFirst().getId());
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

    private List<UserOnboardingTag> preferenceTags(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userOnboardingTagRepository.findAllByUserIdWithTag(userId);
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
