package com.jjikmeok.app.domain.favorite.service;

import com.jjikmeok.app.domain.favorite.dto.request.FavoriteRequest;
import com.jjikmeok.app.domain.favorite.dto.response.FavoriteResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.favorite.entity.Favorite;
import com.jjikmeok.app.domain.favorite.repository.FavoriteRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.enums.RegionDepth;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    FavoriteRepository favoriteRepository;
    @Mock ActivityRepository activityRepository;
    @Mock UserRepository userRepository;
    FavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FavoriteServiceImpl(favoriteRepository, activityRepository, userRepository);
    }

    @Test
    void getFavorites_returnsUserFavorites() {
        User user = user(1L);
        Activity activity = activity(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(favorite(3L, user, activity)));

        List<FavoriteResponse> responses = service.getFavorites(1L);

        assertThat(responses.getFirst().activityId()).isEqualTo(2L);
    }

    @Test
    void createFavorite_increasesLikeCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(favoriteRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> {
            Favorite saved = invocation.getArgument(0);
            setId(saved, 3L);
            return saved;
        });

        service.createFavorite(1L, new FavoriteRequest(2L));

        assertThat(activity.getLikeCount()).isEqualTo(1);
    }

    @Test
    void createFavorite_whenDuplicate_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity(2L)));
        when(favoriteRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createFavorite(1L, new FavoriteRequest(2L)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_FAVORITE_DUPLICATE);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void deleteFavorite_decreasesLikeCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        activity.increaseLikeCount();
        Favorite favorite = favorite(3L, user, activity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(favoriteRepository.findByUserIdAndActivityId(1L, 2L)).thenReturn(Optional.of(favorite));

        service.deleteFavorite(1L, 2L);

        assertThat(activity.getLikeCount()).isZero();
        verify(favoriteRepository).delete(favorite);
    }

    private User user(Long id) {
        User user = User.createForSignup("user@example.com", "hash");
        setId(user, id);
        return user;
    }

    private Activity activity(Long id) {
        Region region = Region.builder().name("서울").depth(RegionDepth.PROVINCE).build();
        setId(region, 10L);
        Activity activity = Activity.builder()
                .region(region).title("활동").sourceUrl("https://example.com").address("장소")
                .recruitEndAt(LocalDateTime.now())
                .price(0).description("설명").isActive(true).build();
        setId(activity, id);
        return activity;
    }

    private Favorite favorite(Long id, User user, Activity activity) {
        Favorite favorite = Favorite.create(user, activity);
        setId(favorite, id);
        return favorite;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
