package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.converter.ActivityPageConverter;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCardResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityHomePageResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivitySectionResponse;
import com.jjikmeok.app.domain.activity.dto.response.page.ActivityShortcutResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.image.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
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
public class ActivityPageServiceImpl implements ActivityPageService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_HOME_LIMIT = 10;
    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int SORT_FETCH_LIMIT = 100;
    private static final ApprovalStatus PUBLIC_STATUS = ApprovalStatus.APPROVED;

    private final ActivityRepository activityRepository;
    private final FavoriteRepository activityFavoriteRepository;
    private final ActivityImageRepository activityImageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserOnboardingTagRepository userOnboardingTagRepository;
    private final UserOnboardingRegionRepository userOnboardingRegionRepository;

    @Override
    public ActivityHomePageResponse getHomePage(Long userId, Integer limit) {
        int size = limit(limit, DEFAULT_HOME_LIMIT);
        String nickname = nickname(userId);

        List<ActivityCardResponse> recommended = cards(userId, recommendedActivities(userId, size), size);
        List<ActivityCardResponse> closingSoon = cards(
                userId,
                activityRepository.findApprovedClosingSoon(PUBLIC_STATUS, LocalDateTime.now(SEOUL), PageRequest.of(0, size)),
                size
        );

        return new ActivityHomePageResponse(
                nickname,
                new ActivityHomePageResponse.Hero(
                        nickname + "님, 오늘은 뭐 찍먹해볼까요?",
                        "확신이 없어도 괜찮아요. 맞는 활동부터 가볍게 둘러보세요.",
                        "나만의 경험 탐색하기",
                        "/activities/pages/custom"
                ),
                shortcuts(),
                new ActivitySectionResponse("recommended", nickname + "님에게 추천해요!", null, recommended),
                new ActivitySectionResponse("closingSoon", "인기 마감 임박", "신청 마감이 가까운 활동이에요.", closingSoon)
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
                type == null ? "카테고리" : type.getLabel(),
                type,
                category,
                selectedSort,
                totalCount,
                typeTabs(type),
                categoryChips(category),
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
                        nickname + "님과 취향 적합도가 높은 활동을 모아봤어요!",
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
        List<ActivityImage> images = activityImageRepository.findAllByActivityIdOrderBySortOrderAscIdAsc(activityId);
        boolean liked = userId != null && activityFavoriteRepository.existsByUserIdAndActivityId(userId, activityId);

        return ActivityPageConverter.toDetail(activity, images, liked, LocalDate.now(SEOUL));
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
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> ActivityPageConverter.toCard(activity, likedActivityIds.contains(activity.getId()), today))
                .toList();
    }

    private Set<Long> likedActivityIds(Long userId, List<Activity> activities) {
        if (userId == null || activities.isEmpty()) {
            return Set.of();
        }

        List<Long> activityIds = activities.stream()
                .map(Activity::getId)
                .toList();

        return Set.copyOf(activityFavoriteRepository.findActivityIdsByUserIdAndActivityIdIn(userId, activityIds));
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
            return "닉네임";
        }
        return userProfileRepository.findByUserId(userId)
                .map(userProfile -> userProfile.getNickname())
                .orElse("닉네임");
    }

    private List<UserOnboardingTag> preferenceTags(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userOnboardingTagRepository.findAllByUserIdWithTag(userId);
    }

    private String tasteTitle(List<String> hashtags) {
        if (hashtags.isEmpty()) {
            return "우선 한 입만 먹어보는 형";
        }
        if (hashtags.contains("#도전") || hashtags.contains("#몰입")) {
            return "꽂히면 깊게 파보는 형";
        }
        if (hashtags.contains("#사교")) {
            return "같이 하면 더 즐거운 형";
        }
        if (hashtags.contains("#힐링") || hashtags.contains("#휴식")) {
            return "천천히 충전하는 형";
        }
        return "우선 한 입만 먹어보는 형";
    }

    private List<ActivityShortcutResponse> shortcuts() {
        return List.of(
                new ActivityShortcutResponse(ActivityType.PROGRAM, ActivityType.PROGRAM.getLabel(), "palette", "/activities/pages/category?type=PROGRAM"),
                new ActivityShortcutResponse(ActivityType.ONE_DAY, ActivityType.ONE_DAY.getLabel(), "clock", "/activities/pages/category?type=ONE_DAY"),
                new ActivityShortcutResponse(ActivityType.EVENT, ActivityType.EVENT.getLabel(), "megaphone", "/activities/pages/category?type=EVENT"),
                new ActivityShortcutResponse(ActivityType.CLUB, ActivityType.CLUB.getLabel(), "users", "/activities/pages/category?type=CLUB")
        );
    }

    private List<ActivityFilterOptionResponse> typeTabs(ActivityType selectedType) {
        List<ActivityFilterOptionResponse> options = new java.util.ArrayList<>();
        options.add(new ActivityFilterOptionResponse("", "전체", selectedType == null));
        for (ActivityType type : ActivityType.values()) {
            options.add(new ActivityFilterOptionResponse(type.name(), type.getLabel(), type == selectedType));
        }
        return options;
    }

    private List<ActivityFilterOptionResponse> categoryChips(ActivityCategory selectedCategory) {
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
                new ActivityFilterOptionResponse("deadline", "마감임박순", "deadline".equals(selectedSort)),
                new ActivityFilterOptionResponse("popular", "인기순", "popular".equals(selectedSort))
        );
    }
}
