package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.entity.ActivityPreferenceVector;
import com.jjikmeok.app.domain.personalization.entity.UserPreferenceVector;
import com.jjikmeok.app.domain.personalization.repository.ActivityPreferenceVectorRepository;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import com.jjikmeok.app.domain.personalization.repository.UserPreferenceVectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    @Mock private UserPreferenceVectorRepository userPreferenceVectorRepository;
    @Mock private ActivityPreferenceVectorRepository activityPreferenceVectorRepository;
    @InjectMocks private PersonlizationService personlizationService;

    @Test
    void getRecommendedActivities_filtersScoresAtOrBelowSixtyAndSortsDescending() {
        UserPreferenceVector userVector = userVector(vector(1.0f, 0.0f));
        ActivityPreferenceVector identical = activityVector(FIRST_ACTIVITY_ID, vector(1.0f, 0.0f));
        ActivityPreferenceVector eightyPoints = activityVector(SECOND_ACTIVITY_ID, vector(0.8f, 0.6f));
        ActivityPreferenceVector sixtyPoints = activityVector(THIRD_ACTIVITY_ID, vector(0.6f, 0.8f));

        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userVector));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(SECOND_ACTIVITY_ID, "Second", null, "craft"),
                recommendation(THIRD_ACTIVITY_ID, "Third", null, "sports"),
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, "healing"),
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, "rest")
        ));
        when(activityPreferenceVectorRepository.findAllByActivityIdIn(
                List.of(SECOND_ACTIVITY_ID, THIRD_ACTIVITY_ID, FIRST_ACTIVITY_ID)))
                .thenReturn(List.of(identical, eightyPoints, sixtyPoints));

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).extracting(ActivityRecommendationResponse::id)
                .containsExactly(FIRST_ACTIVITY_ID, SECOND_ACTIVITY_ID);
        assertThat(responses).extracting(ActivityRecommendationResponse::personalizationScore)
                .containsExactly(100, 80);
        assertThat(responses.getFirst().tags()).containsExactly("healing", "rest");
    }

    @Test
    void getRecommendedActivities_returnsNoCandidatesWhenUserVectorIsMissing() {
        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(FIRST_ACTIVITY_ID, "First", 10L, "healing")
        ));

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).isEmpty();
        verifyNoInteractions(activityPreferenceVectorRepository);
    }

    @Test
    void getRecommendedActivities_excludesZeroActivityVector() {
        UserPreferenceVector userVector = userVector(vector(1.0f, 0.0f));
        ActivityPreferenceVector zeroVector = mock(ActivityPreferenceVector.class);
        when(zeroVector.getEmbeddingCopy()).thenReturn(vector(0.0f, 0.0f));
        when(userPreferenceVectorRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(userVector));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(FIRST_ACTIVITY_ID, "First", null, "healing")
        ));
        when(activityPreferenceVectorRepository.findAllByActivityIdIn(List.of(FIRST_ACTIVITY_ID)))
                .thenReturn(List.of(zeroVector));

        assertThat(personlizationService.getRecommendedActivities(USER_ID)).isEmpty();
    }

    @Test
    void getRecommendedActivities_returnsNoCandidatesWhenVectorLookupFails() {
        when(userPreferenceVectorRepository.findByUserId(USER_ID))
                .thenThrow(new InvalidDataAccessResourceUsageException("table unavailable"));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(FIRST_ACTIVITY_ID, "First", null, "healing")
        ));

        assertThat(personlizationService.getRecommendedActivities(USER_ID)).isEmpty();
        verifyNoInteractions(activityPreferenceVectorRepository);
    }

    private UserPreferenceVector userVector(float[] embedding) {
        UserPreferenceVector vector = mock(UserPreferenceVector.class);
        when(vector.getEmbeddingCopy()).thenReturn(embedding);
        return vector;
    }

    private ActivityPreferenceVector activityVector(Long activityId, float[] embedding) {
        Activity activity = mock(Activity.class);
        when(activity.getId()).thenReturn(activityId);
        ActivityPreferenceVector vector = mock(ActivityPreferenceVector.class);
        when(vector.getActivity()).thenReturn(activity);
        when(vector.getEmbeddingCopy()).thenReturn(embedding);
        return vector;
    }

    private float[] vector(float first, float second) {
        float[] values = new float[14];
        values[0] = first;
        values[1] = second;
        return values;
    }

    private ActivityRecommendationProjection recommendation(
            Long activityId,
            String title,
            Long activityFavoriteId,
            String tagName
    ) {
        return new TestActivityRecommendationProjection(
                activityId,
                title,
                "https://example.com/" + activityId + ".png",
                RECRUIT_END_AT,
                activityFavoriteId,
                tagName
        );
    }

    private record TestActivityRecommendationProjection(
            Long activityId,
            String title,
            String thumbnailUrl,
            LocalDateTime recruitEndAt,
            Long activityFavoriteId,
            String tagName
    ) implements ActivityRecommendationProjection {
        @Override public Long getActivityId() { return activityId; }
        @Override public String getTitle() { return title; }
        @Override public String getThumbnailUrl() { return thumbnailUrl; }
        @Override public LocalDateTime getRecruitEndAt() { return recruitEndAt; }
        @Override public Long getActivityFavoriteId() { return activityFavoriteId; }
        @Override public String getTagName() { return tagName; }
    }
}
