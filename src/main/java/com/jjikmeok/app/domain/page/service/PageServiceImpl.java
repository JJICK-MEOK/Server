package com.jjikmeok.app.domain.page.service;

import com.jjikmeok.app.domain.page.converter.PageConverter;
import com.jjikmeok.app.domain.page.dto.response.ActivityCardResponse;
import com.jjikmeok.app.domain.page.dto.response.CategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.CustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFilterOptionResponse;
import com.jjikmeok.app.domain.page.dto.response.HomePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityListItemResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivitySectionResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityShortcutResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.image.entity.ActivityImage;
import com.jjikmeok.app.domain.image.repository.ActivityImageRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.user.repository.UserOnboardingRegionRepository;
import com.jjikmeok.app.domain.user.repository.UserOnboardingTagRepository;
import com.jjikmeok.app.domain.user.repository.UserProfileRepository;
import com.jjikmeok.app.domain.user.repository.UserRepository;
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
    private static final int HOME_RECOMMENDED_LIMIT = 3;
    private static final int HOME_CLOSING_SOON_LIMIT = 3;
    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int SORT_FETCH_LIMIT = 100;
    private static final ApprovalStatus PUBLIC_STATUS = ApprovalStatus.APPROVED;

    private final ActivityRepository activityRepository;
    private final FavoriteRepository activityFavoriteRepository;
    private final ActivityImageRepository activityImageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserOnboardingTagRepository userOnboardingTagRepository;
    private final UserOnboardingRegionRepository userOnboardingRegionRepository;

    @Override
    public HomePageResponse getHomePage(Long userId, Integer limit) {
        String nickname = nickname(userId);

        // TODO: 추천 정책이 최종 확정되면, 현재의 임시(Placeholder) 추천 로직을 태그 겹침(Overlap) 점수 산정 방식으로 대체할 것.
        List<HomePageResponse.RecommendedActivity> recommended = homeRecommendedActivities(
                userId,
                recommendedActivities(userId, HOME_RECOMMENDED_LIMIT),
                HOME_RECOMMENDED_LIMIT
        );
        List<HomePageResponse.ClosingSoonActivity> closingSoon = homeClosingSoonActivities(
                userId,
                activityRepository.findApprovedClosingSoon(PUBLIC_STATUS, LocalDateTime.now(SEOUL), PageRequest.of(0, HOME_CLOSING_SOON_LIMIT)),
                HOME_CLOSING_SOON_LIMIT
        );

        return new HomePageResponse(
                nickname,
                new HomePageResponse.Banner(
                        "확신이 없어도 괜찮아요 일단 찍먹 해보세요!",
                        "다양한 활동을 부담 없이 탐색해보세요",
                        "나만의 경험 탐색하기",
                        "/pages/custom",
                        1,
                        2
                ),
                shortcuts(),
                new HomePageResponse.RecommendedSection(nickname + " 님에게 추천해요!", "/pages/custom", recommended),
                new HomePageResponse.ClosingSoonSection("인기 마감 임박", "/pages/category?sort=deadline", "dark", closingSoon)
        );
    }

    @Override
    public CategoryPageResponse getCategoryPage(
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

        return new CategoryPageResponse(
                type == null ? "카테고리" : type.getLabel(),
                type,
                category,
                selectedSort,
                totalCount,
                typeTabs(type),
                categoryChips(category),
                sortOptions(selectedSort),
                listItems(userId, sorted, size)
        );
    }

    @Override
    public CustomPageResponse getCustomPage(Long userId, Integer limit) {
        int size = limit(limit, DEFAULT_HOME_LIMIT);
        String nickname = nickname(userId);
        List<UserOnboardingTag> preferenceTags = preferenceTags(userId);
        List<String> hashtags = preferenceTags.stream()
                .map(userOnboardingTag -> "#" + userOnboardingTag.getTag().getName())
                .toList();

        // TODO: 현재의 임시 맞춤형 피드를 최종 확정된 유사/확장/빠른 탐색(Quick-Explore) 전략으로 대체할 것.
        List<ActivityCardResponse> recommended = cards(userId, recommendedActivities(userId, size), size);

        return new CustomPageResponse(
                nickname,
                new CustomPageResponse.TasteProfile(
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

        return PageConverter.toDetail(activity, images, liked, LocalDate.now(SEOUL));
    }

    private List<Activity> recommendedActivities(Long userId, int size) {
        // TODO: 현재의 폴백(Fallback, 예외 처리용 고정 순서) 방식을 사용하는 대신, 사용자의 선호 태그, 토픽 태그, 활동 유형이 얼마나 겹치는지 계산하여 점수를 매길 것.
        List<Long> tagIds = preferenceTags(userId).stream()
                .map(userOnboardingTag -> userOnboardingTag.getTag().getId())
                .toList();

        if (!tagIds.isEmpty()) {
            List<Activity> byTags = activityRepository.findRecommendedByPreferenceTagIds(
                    PUBLIC_STATUS,
                    tagIds,
                    LocalDate.now(SEOUL).atStartOfDay(),
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
                    LocalDate.now(SEOUL).atStartOfDay(),
                    PageRequest.of(0, size)
            );
            if (!byRegions.isEmpty()) {
                return byRegions;
            }
        }

        return activityRepository.findApprovedLatest(PUBLIC_STATUS, LocalDate.now(SEOUL).atStartOfDay(), PageRequest.of(0, size));
    }

    private List<ActivityCardResponse> cards(Long userId, List<Activity> activities, int limit) {
        List<Activity> distinctActivities = distinct(activities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> PageConverter.toCard(activity, likedActivityIds.contains(activity.getId()), today))
                .toList();
    }

    private List<ActivityListItemResponse> listItems(Long userId, List<Activity> activities, int limit) {
        List<Activity> distinctActivities = distinct(activities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> {
                    ActivityCardResponse card = PageConverter.toCard(activity, likedActivityIds.contains(activity.getId()), today);
                    return new ActivityListItemResponse(
                            card.id(),
                            card.title(),
                            card.thumbnailUrl(),
                            card.dDay(),
                            card.viewCount(),
                            card.likeCount(),
                            card.hashtags(),
                            card.liked()
                    );
                })
                .toList();
    }

    private List<HomePageResponse.RecommendedActivity> homeRecommendedActivities(Long userId, List<Activity> activities, int limit) {
        List<Activity> distinctActivities = distinct(activities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> {
                    ActivityCardResponse card = PageConverter.toCard(activity, likedActivityIds.contains(activity.getId()), today);
                    return new HomePageResponse.RecommendedActivity(
                            card.id(),
                            card.title(),
                            card.thumbnailUrl(),
                            card.categoryLabel(),
                            card.dDay(),
                            card.hashtags(),
                            card.liked()
                    );
                })
                .toList();
    }

    private List<HomePageResponse.ClosingSoonActivity> homeClosingSoonActivities(Long userId, List<Activity> activities, int limit) {
        List<Activity> distinctActivities = distinct(activities).stream()
                .limit(limit)
                .toList();
        Set<Long> likedActivityIds = likedActivityIds(userId, distinctActivities);
        LocalDate today = LocalDate.now(SEOUL);

        return distinctActivities.stream()
                .map(activity -> {
                    ActivityCardResponse card = PageConverter.toCard(activity, likedActivityIds.contains(activity.getId()), today);
                    return new HomePageResponse.ClosingSoonActivity(
                            card.id(),
                            card.title(),
                            summary(activity),
                            card.thumbnailUrl(),
                            card.dDay(),
                            card.activityTypeLabel(),
                            card.categoryLabel(),
                            card.liked()
                    );
                })
                .toList();
    }

    private String summary(Activity activity) {
        String description = activity.getDescription();
        if (description == null || description.isBlank()) {
            return activity.getActivityType().getLabel() + " 관련 활동";
        }

        String compact = description.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 32) {
            return compact;
        }
        return compact.substring(0, 32) + "...";
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
                .filter(nickname -> !nickname.isBlank())
                .or(() -> userRepository.findById(userId)
                        .map(User::getEmail)
                        .map(this::displayNameFromEmail))
                .orElse("사용자");
    }

    private String displayNameFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return "사용자";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
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
                new ActivityShortcutResponse(ActivityType.PROGRAM, ActivityType.PROGRAM.getLabel(), "/images/icons/activity-program.png", "/pages/category?type=PROGRAM"),
                new ActivityShortcutResponse(ActivityType.ONE_DAY, ActivityType.ONE_DAY.getLabel(), "/images/icons/activity-one-day.png", "/pages/category?type=ONE_DAY"),
                new ActivityShortcutResponse(ActivityType.EVENT, ActivityType.EVENT.getLabel(), "/images/icons/activity-event.png", "/pages/category?type=EVENT"),
                new ActivityShortcutResponse(ActivityType.CLUB, ActivityType.CLUB.getLabel(), "/images/icons/activity-club.png", "/pages/category?type=CLUB")
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
