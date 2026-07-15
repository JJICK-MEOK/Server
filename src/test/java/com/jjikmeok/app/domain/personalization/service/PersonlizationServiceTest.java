package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import com.jjikmeok.app.domain.tag.entity.TagType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonlizationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FIRST_ACTIVITY_ID = 101L;
    private static final Long SECOND_ACTIVITY_ID = 202L;
    private static final Long THIRD_ACTIVITY_ID = 303L;
    private static final LocalDateTime RECRUIT_END_AT = LocalDateTime.of(2026, 7, 10, 18, 0);

    @Mock private PersonalizationRepository personalizationRepository;
    @InjectMocks private PersonlizationService personlizationService;

    @Test
    void getRecommendedActivities_buildsIntVectorsFromDatabaseTagOrderAndSortsScores() {
        when(personalizationRepository.findPreferenceTagIdsOrderById())
                .thenReturn(List.of(1L, 2L, 3L, 4L));
        when(personalizationRepository.findPreferenceTagIdsByUserId(USER_ID))
                .thenReturn(List.of(1L, 2L));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(SECOND_ACTIVITY_ID, "Second", null, 1L, "calm", TagType.PREFERENCE_TAG),
                recommendation(SECOND_ACTIVITY_ID, "Second", null, 3L, "creative", TagType.PREFERENCE_TAG),
                recommendation(THIRD_ACTIVITY_ID, "Third", null, 3L, "creative", TagType.PREFERENCE_TAG),
                recommendation(THIRD_ACTIVITY_ID, "Third", null, 4L, "trendy", TagType.PREFERENCE_TAG),
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, 1L, "calm", TagType.PREFERENCE_TAG),
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, 2L, "healing", TagType.PREFERENCE_TAG),
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, 99L, "culture", TagType.TOPIC_CATEGORY)
        ));

        List<ActivityRecommendationResponse> responses =
                personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).extracting(ActivityRecommendationResponse::id)
                .containsExactly(FIRST_ACTIVITY_ID, SECOND_ACTIVITY_ID);
        assertThat(responses).extracting(ActivityRecommendationResponse::personalizationScore)
                .containsExactly(100, 50);
        assertThat(responses.getFirst().tags()).containsExactly("calm", "healing", "culture");
    }

    @Test
    void getRecommendedActivities_returnsNoCandidatesWhenUserHasNoPreferenceTags() {
        when(personalizationRepository.findPreferenceTagIdsOrderById()).thenReturn(List.of(1L));
        when(personalizationRepository.findPreferenceTagIdsByUserId(USER_ID)).thenReturn(List.of());
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(
                        FIRST_ACTIVITY_ID,
                        "First",
                        10L,
                        1L,
                        "healing",
                        TagType.PREFERENCE_TAG
                )
        ));

        assertThat(personlizationService.getRecommendedActivities(USER_ID)).isEmpty();
    }

    @Test
    void getRecommendedActivities_excludesActivityWithoutPreferenceTags() {
        when(personalizationRepository.findPreferenceTagIdsOrderById()).thenReturn(List.of(1L));
        when(personalizationRepository.findPreferenceTagIdsByUserId(USER_ID)).thenReturn(List.of(1L));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(
                        FIRST_ACTIVITY_ID,
                        "First",
                        null,
                        99L,
                        "culture",
                        TagType.TOPIC_CATEGORY
                )
        ));

        assertThat(personlizationService.getRecommendedActivities(USER_ID)).isEmpty();
    }

    @Test
    void getRecommendedActivities_returnsEmptyWithoutQueryingDatabaseForNullUserId() {
        assertThat(personlizationService.getRecommendedActivities(null)).isEmpty();
        verifyNoInteractions(personalizationRepository);
    }

    private ActivityRecommendationProjection recommendation(
            Long activityId,
            String title,
            Long activityFavoriteId,
            Long tagId,
            String tagName,
            TagType tagType
    ) {
        return new TestActivityRecommendationProjection(
                activityId,
                title,
                "https://example.com/" + activityId + ".png",
                RECRUIT_END_AT,
                activityFavoriteId,
                tagId,
                tagName,
                tagType.name()
        );
    }

    private record TestActivityRecommendationProjection(
            Long activityId,
            String title,
            String thumbnailUrl,
            LocalDateTime recruitEndAt,
            Long activityFavoriteId,
            Long tagId,
            String tagName,
            String tagType
    ) implements ActivityRecommendationProjection {
        @Override public Long getActivityId() { return activityId; }
        @Override public String getTitle() { return title; }
        @Override public String getThumbnailUrl() { return thumbnailUrl; }
        @Override public LocalDateTime getRecruitEndAt() { return recruitEndAt; }
        @Override public Long getActivityFavoriteId() { return activityFavoriteId; }
        @Override public Long getTagId() { return tagId; }
        @Override public String getTagName() { return tagName; }
        @Override public String getTagType() { return tagType; }
    }
}
