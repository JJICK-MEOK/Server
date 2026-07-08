package com.jjikmeok.app.domain.favorite.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.favorite.entity.Favorite;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    private FavoriteServiceImpl favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteServiceImpl(favoriteRepository, activityRepository, userRepository);
    }

    @Test
    void getFavorites_returnsCardsWithTwoHashtags() {
        User user = User.createForSignup("tester@example.com", "hash");
        setId(user, 1L);
        Activity activity = activity();
        Favorite favorite = Favorite.create(user, activity);
        setId(favorite, 11L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(favorite));

        assertThat(favoriteService.getFavorites(1L, "saved"))
                .hasSize(1)
                .first()
                .satisfies(card -> assertThat(card.hashtags()).hasSize(2));
    }

    private Activity activity() {
        Region region = Region.builder()
                .name("서울")
                .depth(RegionDepth.PROVINCE)
                .build();
        setId(region, 10L);

        Activity activity = Activity.builder()
                .region(region)
                .title("테스트 활동")
                .description("상세 설명")
                .thumbnailUrl("https://example.com/thumb.png")
                .sourceUrl("https://example.com/apply")
                .address("서울")
                .recruitStartAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .recruitEndAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .startAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .endAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .activityType(ActivityType.PROGRAM)
                .category(ActivityCategory.CRAFT)
                .sourceType(SourceType.URL_MANUAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .price(0)
                .isActive(true)
                .build();
        setId(activity, 2L);
        return activity;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
