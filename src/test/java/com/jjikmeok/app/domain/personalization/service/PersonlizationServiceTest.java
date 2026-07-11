package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.dto.ActivityPersonalizationScoreProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.entity.UserPreferenceVector;
import com.jjikmeok.app.domain.personalization.repository.ActivityPreferenceVectorRepository;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import com.jjikmeok.app.domain.personalization.repository.UserPreferenceVectorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonlizationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long FIRST_ACTIVITY_ID = 101L;
    private static final Long SECOND_ACTIVITY_ID = 202L;
    private static final LocalDateTime RECRUIT_END_AT = LocalDateTime.of(2026, 7, 10, 18, 0);

    @Mock
    private PersonalizationRepository personalizationRepository;

    @Mock
    private UserPreferenceVectorRepository userPreferenceVectorRepository;

    @Mock
    private ActivityPreferenceVectorRepository activityPreferenceVectorRepository;

    @InjectMocks
    private PersonlizationService personlizationService;

    @Test
    void getRecommendedActivities_loadsUserVectorThenCandidatesAndMapsScoresByActivityId() {
        UserPreferenceVector userPreferenceVector = mock(UserPreferenceVector.class);
        List<ActivityRecommendationProjection> candidates = List.of(
                recommendation(FIRST_ACTIVITY_ID, "First activity", 10L, "healing"),
                recommendation(FIRST_ACTIVITY_ID, "First activity", 10L, "rest"),
                recommendation(SECOND_ACTIVITY_ID, "Second activity", null, "craft")
        );

        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userPreferenceVector));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(candidates);
        when(activityPreferenceVectorRepository.findPersonalizationScores(
                USER_ID,
                List.of(FIRST_ACTIVITY_ID, SECOND_ACTIVITY_ID)
        )).thenReturn(List.of(new TestActivityPersonalizationScoreProjection(FIRST_ACTIVITY_ID, 90)));

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).extracting(ActivityRecommendationResponse::id)
                .containsExactly(FIRST_ACTIVITY_ID, SECOND_ACTIVITY_ID);
        assertThat(responses.getFirst().personalizationScore()).isEqualTo(90);
        assertThat(responses.getFirst().tags()).containsExactly("healing", "rest");
        assertThat(responses.get(1).personalizationScore()).isNull();
        assertThat(responses.get(1).tags()).containsExactly("craft");

        InOrder inOrder = inOrder(
                userPreferenceVectorRepository,
                personalizationRepository,
                activityPreferenceVectorRepository
        );
        inOrder.verify(userPreferenceVectorRepository).findByUserId(USER_ID);
        inOrder.verify(personalizationRepository).findRecommendedActivitiesByUserId(USER_ID);
        inOrder.verify(activityPreferenceVectorRepository).findPersonalizationScores(
                USER_ID,
                List.of(FIRST_ACTIVITY_ID, SECOND_ACTIVITY_ID)
        );
    }

    @Test
    void getRecommendedActivities_returnsExistingCandidatesWithNullScoreWhenUserVectorIsMissing() {
        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(FIRST_ACTIVITY_ID, "First activity", 10L, "healing")
        ));

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(FIRST_ACTIVITY_ID);
        assertThat(responses.getFirst().personalizationScore()).isNull();
        assertThat(responses.getFirst().tags()).containsExactly("healing");

        InOrder inOrder = inOrder(userPreferenceVectorRepository, personalizationRepository);
        inOrder.verify(userPreferenceVectorRepository).findByUserId(USER_ID);
        inOrder.verify(personalizationRepository).findRecommendedActivitiesByUserId(USER_ID);
        verifyNoInteractions(activityPreferenceVectorRepository);
    }

    @Test
    void getRecommendedActivities_returnsNullScoreWhenScoreQueryReturnsNull() {
        UserPreferenceVector userPreferenceVector = mock(UserPreferenceVector.class);
        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userPreferenceVector));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of(
                recommendation(FIRST_ACTIVITY_ID, "First activity", 10L, "healing")
        ));
        when(activityPreferenceVectorRepository.findPersonalizationScores(
                USER_ID,
                List.of(FIRST_ACTIVITY_ID)
        )).thenReturn(List.of(new TestActivityPersonalizationScoreProjection(FIRST_ACTIVITY_ID, null)));

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().personalizationScore()).isNull();
        assertThat(responses.getFirst().tags()).containsExactly("healing");
    }

    @Test
    void getRecommendedActivities_doesNotQueryScoresWhenCandidatesAreEmpty() {
        UserPreferenceVector userPreferenceVector = mock(UserPreferenceVector.class);
        when(userPreferenceVectorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userPreferenceVector));
        when(personalizationRepository.findRecommendedActivitiesByUserId(USER_ID)).thenReturn(List.of());

        List<ActivityRecommendationResponse> responses = personlizationService.getRecommendedActivities(USER_ID);

        assertThat(responses).isEmpty();

        InOrder inOrder = inOrder(userPreferenceVectorRepository, personalizationRepository);
        inOrder.verify(userPreferenceVectorRepository).findByUserId(USER_ID);
        inOrder.verify(personalizationRepository).findRecommendedActivitiesByUserId(USER_ID);
        verifyNoInteractions(activityPreferenceVectorRepository);
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

        @Override
        public Long getActivityId() {
            return activityId;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        @Override
        public LocalDateTime getRecruitEndAt() {
            return recruitEndAt;
        }

        @Override
        public Long getActivityFavoriteId() {
            return activityFavoriteId;
        }

        @Override
        public String getTagName() {
            return tagName;
        }
    }

    private record TestActivityPersonalizationScoreProjection(
            Long activityId,
            Integer personalizationScore
    ) implements ActivityPersonalizationScoreProjection {

        @Override
        public Long getActivityId() {
            return activityId;
        }

        @Override
        public Integer getPersonalizationScore() {
            return personalizationScore;
        }
    }
}
