package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityTag;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.favorite.entity.Favorite;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboarding;
import com.jjikmeok.app.domain.onboarding.entity.UserOnboardingTag;
import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationProjection;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:personalization;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PersonalizationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 3, 0, 0);

    @Autowired
    private PersonalizationRepository personalizationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findTagNamesByUserId_usesUserOnboardingsTable() {
        User user = entityManager.persist(User.createForSignup("user@example.com", "password"));
        UserOnboarding onboarding = entityManager.persist(UserOnboarding.create(user));
        Tag tag = entityManager.persist(Tag.create("healing", TagType.PREFERENCE_TAG));
        entityManager.persist(UserOnboardingTag.create(onboarding, tag));
        entityManager.flush();
        entityManager.clear();

        List<String> tagNames = personalizationRepository.findTagNamesByUserId(user.getId());

        assertThat(tagNames).containsExactly("healing");
    }

    @Test
    void preferenceTagIdQueries_useDatabaseIdOrderAndExcludeOtherTagTypes() {
        User user = entityManager.persist(User.createForSignup("vector-order@example.com", "password"));
        UserOnboarding onboarding = entityManager.persist(UserOnboarding.create(user));
        entityManager.persist(Tag.create("culture", TagType.TOPIC_CATEGORY));
        Tag healing = entityManager.persist(Tag.create("healing", TagType.PREFERENCE_TAG));
        Tag calm = entityManager.persist(Tag.create("calm", TagType.PREFERENCE_TAG));
        entityManager.persist(UserOnboardingTag.create(onboarding, calm));
        entityManager.persist(UserOnboardingTag.create(onboarding, healing));
        entityManager.flush();
        entityManager.clear();

        assertThat(personalizationRepository.findPreferenceTagIdsOrderById())
                .containsExactly(healing.getId(), calm.getId());
        assertThat(personalizationRepository.findPreferenceTagIdsByUserId(user.getId()))
                .containsExactly(healing.getId(), calm.getId());
    }

    @Test
    void findRecommendedActivitiesByUserId_returnsProjectionWithoutNativeSqlError() {
        User user = entityManager.persist(User.createForSignup("recommended@example.com", "password"));
        UserOnboarding onboarding = entityManager.persist(UserOnboarding.create(user));
        Region region = entityManager.persist(Region.builder()
                .name("Seoul")
                .depth(RegionDepth.PROVINCE)
                .build());

        Tag healing = entityManager.persist(Tag.create("healing", TagType.PREFERENCE_TAG));
        Tag rest = entityManager.persist(Tag.create("rest", TagType.PREFERENCE_TAG));
        entityManager.persist(UserOnboardingTag.create(onboarding, healing));
        entityManager.persist(UserOnboardingTag.create(onboarding, rest));

        Activity activity = entityManager.persist(activity(region, "Recommended"));
        entityManager.persist(ActivityTag.create(activity, healing));
        entityManager.persist(ActivityTag.create(activity, rest));
        Favorite favorite = entityManager.persist(Favorite.create(user, activity));
        entityManager.flush();
        entityManager.clear();

        List<ActivityRecommendationProjection> recommendations =
                personalizationRepository.findRecommendedActivitiesByUserId(user.getId());

        assertThat(recommendations).hasSize(2);
        ActivityRecommendationProjection recommendation = recommendations.getFirst();
        assertThat(recommendation.getActivityId()).isEqualTo(activity.getId());
        assertThat(recommendation.getTitle()).isEqualTo("Recommended");
        assertThat(recommendation.getActivityFavoriteId()).isEqualTo(favorite.getId());
        assertThat(recommendation.getTagId()).isEqualTo(healing.getId());
        assertThat(recommendation.getTagType()).isEqualTo(TagType.PREFERENCE_TAG.name());
        assertThat(recommendations)
                .extracting(ActivityRecommendationProjection::getTagName)
                .containsExactly("healing", "rest");
    }

    @Test
    void findRecommendedActivitiesByUserId_sortsByOnboardingTagNamesThenLikeCount() {
        User user = entityManager.persist(User.createForSignup("sorted@example.com", "password"));
        UserOnboarding onboarding = entityManager.persist(UserOnboarding.create(user));
        Region region = entityManager.persist(Region.builder()
                .name("Seoul")
                .depth(RegionDepth.PROVINCE)
                .build());

        Tag healing = entityManager.persist(Tag.create("healing", TagType.PREFERENCE_TAG));
        Tag rest = entityManager.persist(Tag.create("rest", TagType.PREFERENCE_TAG));
        Tag healingTopic = entityManager.persist(Tag.create("healing", TagType.TOPIC_CATEGORY));
        Tag art = entityManager.persist(Tag.create("art", TagType.TOPIC_CATEGORY));
        entityManager.persist(UserOnboardingTag.create(onboarding, healing));
        entityManager.persist(UserOnboardingTag.create(onboarding, rest));

        Activity twoOnboardingMatches = entityManager.persist(activity(region, "Two onboarding matches"));
        entityManager.persist(ActivityTag.create(twoOnboardingMatches, healing));
        entityManager.persist(ActivityTag.create(twoOnboardingMatches, rest));

        Activity moreNameMatches = entityManager.persist(activity(region, "More name matches"));
        entityManager.persist(ActivityTag.create(moreNameMatches, rest));
        entityManager.persist(ActivityTag.create(moreNameMatches, healingTopic));

        Activity higherLikeCount = entityManager.persist(activity(region, "Higher like count"));
        entityManager.persist(ActivityTag.create(higherLikeCount, healing));

        Activity lowerLikeCount = entityManager.persist(activity(region, "Lower like count"));
        entityManager.persist(ActivityTag.create(lowerLikeCount, healing));
        entityManager.persist(ActivityTag.create(lowerLikeCount, art));

        entityManager.flush();
        updateLikeCount(higherLikeCount, 10);
        updateLikeCount(lowerLikeCount, 1);
        entityManager.clear();

        List<ActivityRecommendationProjection> recommendations =
                personalizationRepository.findRecommendedActivitiesByUserId(user.getId());

        assertThat(distinctActivityIds(recommendations))
                .containsExactly(
                        twoOnboardingMatches.getId(),
                        moreNameMatches.getId(),
                        higherLikeCount.getId(),
                        lowerLikeCount.getId()
                );
    }

    private Activity activity(Region region, String title) {
        return Activity.builder()
                .region(region)
                .title(title)
                .description(title + " description")
                .thumbnailUrl("https://example.com/thumb.png")
                .sourceUrl("https://example.com/" + title)
                .address("Seoul")
                .recruitStartAt(NOW.minusDays(1))
                .recruitEndAt(NOW.plusDays(7))
                .startAt(NOW.plusDays(8))
                .endAt(NOW.plusDays(9))
                .price(0)
                .activityType(ActivityType.ONE_DAY)
                .category(ActivityCategory.CRAFT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .isActive(true)
                .build();
    }

    private void updateLikeCount(Activity activity, int likeCount) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE activities SET like_count = ? WHERE id = ?")
                .setParameter(1, likeCount)
                .setParameter(2, activity.getId())
                .executeUpdate();
    }

    private List<Long> distinctActivityIds(List<ActivityRecommendationProjection> recommendations) {
        return recommendations.stream()
                .map(ActivityRecommendationProjection::getActivityId)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }
}
