package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityReviewRequest;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityReview;
import com.jjikmeok.app.domain.activity.enums.AgeRange;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityReviewRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityReviewServiceImplTest {

    @Mock ActivityReviewRepository reviewRepository;
    @Mock ActivityRepository activityRepository;
    @Mock UserRepository userRepository;
    ActivityReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActivityReviewServiceImpl(reviewRepository, activityRepository, userRepository);
    }

    @Test
    void createReview_increasesReviewCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(ActivityReview.class))).thenAnswer(invocation -> {
            ActivityReview saved = invocation.getArgument(0);
            setId(saved, 3L);
            return saved;
        });

        service.createReview(1L, 2L, new ActivityReviewRequest(5, "좋아요"));

        assertThat(activity.getReviewCount()).isEqualTo(1);
    }

    @Test
    void createReview_whenRatingInvalid_throwsBadRequest() {
        CustomException exception = assertThrows(CustomException.class,
                () -> service.createReview(1L, 2L, new ActivityReviewRequest(0, "bad")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_INVALID_RATING);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createReview_whenDuplicate_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity(2L)));
        when(reviewRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createReview(1L, 2L, new ActivityReviewRequest(4, "중복")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
    }

    @Test
    void updateReview_updatesOwnedReview() {
        User user = user(1L);
        Activity activity = activity(2L);
        ActivityReview review = review(3L, user, activity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.of(review));

        var response = service.updateReview(1L, 2L, 3L, new ActivityReviewRequest(3, "수정"));

        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.reason()).isEqualTo("수정");
    }

    @Test
    void deleteReview_decreasesReviewCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        activity.increaseReviewCount();
        ActivityReview review = review(3L, user, activity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.of(review));

        service.deleteReview(1L, 2L, 3L);

        assertThat(activity.getReviewCount()).isZero();
        verify(reviewRepository).delete(review);
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
                .region(region).title("활동").uri("https://example.com")
                .recruitEndAt(LocalDateTime.now()).ageRange(AgeRange.ANYONE)
                .price(0).description("설명").isActive(true).build();
        setId(activity, id);
        return activity;
    }

    private ActivityReview review(Long id, User user, Activity activity) {
        ActivityReview review = ActivityReview.create(user, activity, 5, "좋아요");
        setId(review, id);
        return review;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
