package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.dto.ActivityPersonalizationScoreProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.repository.ActivityPreferenceVectorRepository;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import com.jjikmeok.app.domain.personalization.repository.UserPreferenceVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonlizationService {

    private static final int DISPLAY_TAG_LIMIT = 4;

    private final PersonalizationRepository personalizationRepository;
    private final UserPreferenceVectorRepository userPreferenceVectorRepository;
    private final ActivityPreferenceVectorRepository activityPreferenceVectorRepository;

    public List<String> findTags(Long userId) {
        return personalizationRepository.findTagNamesByUserId(userId);
    }

    public PersonalizationResponse findBestType(Long userId) {
        List<String> userTags = findTags(userId);

        if (userTags == null || userTags.isEmpty()) {
            return new PersonalizationResponse("분류 불가", List.of());
        }

        Set<String> userTagSet = userTags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.replace("#", "").trim())
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toSet());

        if (userTagSet.isEmpty()) {
            return new PersonalizationResponse("분류 불가", pickRandomDisplayTags(userTags));
        }

        Map<String, List<String>> typeTags = new LinkedHashMap<>();
        typeTags.put("편안하게 쉬는 타입", List.of("편안한", "힐링", "휴식", "가볍게", "단기"));
        typeTags.put("감성 충전 타입", List.of("감성적", "창의적", "취미", "배움", "한달"));
        typeTags.put("배움 성장 타입", List.of("입문", "몰입", "배움", "성장", "한달"));
        typeTags.put("활기 도전 타입", List.of("활기찬", "트렌디", "도전", "입문", "단기"));
        typeTags.put("소규모 몰입 타입", List.of("몰입", "가볍게", "소규모", "단기"));
        typeTags.put("대규모 트렌드 타입", List.of("활기찬", "트렌디", "대규모", "취미"));

        String bestType = "분류 불가";
        long bestMatchCount = 0;

        for (Map.Entry<String, List<String>> entry : typeTags.entrySet()) {
            long matchCount = entry.getValue().stream()
                    .filter(userTagSet::contains)
                    .count();

            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                bestType = entry.getKey();
            }
        }

        return new PersonalizationResponse(bestType, pickRandomDisplayTags(userTags));
    }

    private List<String> pickRandomDisplayTags(List<String> userTags) {
        if (userTags.size() <= DISPLAY_TAG_LIMIT) {
            return userTags;
        }

        List<String> shuffledTags = new ArrayList<>(userTags);
        Collections.shuffle(shuffledTags);
        return shuffledTags.subList(0, DISPLAY_TAG_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<ActivityRecommendationResponse> getRecommendedActivities(Long userId) {
        boolean hasUserVector = hasUserPreferenceVector(userId);
        Map<Long, ActivityRecommendationAccumulator> recommendations = new LinkedHashMap<>();

        for (ActivityRecommendationProjection projection : personalizationRepository.findRecommendedActivitiesByUserId(userId)) {
            ActivityRecommendationAccumulator accumulator = recommendations.computeIfAbsent(
                    projection.getActivityId(),
                    ignored -> new ActivityRecommendationAccumulator(projection)
            );
            accumulator.addTag(projection.getTagName());
        }

        if (hasUserVector && !recommendations.isEmpty()) {
            Map<Long, Integer> scoresByActivityId = findPersonalizationScores(
                    userId,
                    new ArrayList<>(recommendations.keySet())
            );

            recommendations.forEach((activityId, accumulator) ->
                    accumulator.setPersonalizationScore(scoresByActivityId.get(activityId))
            );
        }

        return recommendations.values()
                .stream()
                .map(ActivityRecommendationAccumulator::toResponse)
                .toList();
    }

    private boolean hasUserPreferenceVector(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            return userPreferenceVectorRepository.findByUserId(userId).isPresent();
        } catch (DataAccessException e) {
            log.warn("User preference vector lookup failed. userId={}, returning recommendations without scores", userId, e);
            return false;
        }
    }

    private Map<Long, Integer> findPersonalizationScores(Long userId, List<Long> activityIds) {
        Map<Long, Integer> scoresByActivityId = new LinkedHashMap<>();

        try {
            for (ActivityPersonalizationScoreProjection score : activityPreferenceVectorRepository.findPersonalizationScores(
                    userId,
                    activityIds
            )) {
                scoresByActivityId.put(score.getActivityId(), score.getPersonalizationScore());
            }
        } catch (DataAccessException e) {
            log.warn(
                    "Activity personalization score query failed. userId={}, activityIds={}, returning null scores",
                    userId,
                    activityIds,
                    e
            );
        }

        return scoresByActivityId;
    }

    private static class ActivityRecommendationAccumulator {
        private final Long id;
        private final String title;
        private final String thumbnailUrl;
        private final LocalDateTime recruitEndAt;
        private final Long activityFavoriteId;
        private final Set<String> tags = new LinkedHashSet<>();
        private Integer personalizationScore;

        private ActivityRecommendationAccumulator(ActivityRecommendationProjection projection) {
            this.id = projection.getActivityId();
            this.title = projection.getTitle();
            this.thumbnailUrl = projection.getThumbnailUrl();
            this.recruitEndAt = projection.getRecruitEndAt();
            this.activityFavoriteId = projection.getActivityFavoriteId();
        }

        private void addTag(String tagName) {
            if (tagName == null || tagName.isBlank()) {
                return;
            }
            tags.add(tagName);
        }

        private void setPersonalizationScore(Integer personalizationScore) {
            this.personalizationScore = personalizationScore;
        }

        private ActivityRecommendationResponse toResponse() {
            return new ActivityRecommendationResponse(
                    id,
                    title,
                    thumbnailUrl,
                    recruitEndAt,
                    activityFavoriteId,
                    personalizationScore,
                    tags.toArray(String[]::new)
            );
        }
    }
}
