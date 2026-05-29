package com.jjikmeok.app.domain.review.service;

import com.jjikmeok.app.domain.review.dto.request.ReviewRequest;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.review.entity.Review;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.review.repository.ReviewRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class ActivityReviewServiceImplTest {

    @Mock
    ReviewRepository reviewRepository;
    @Mock ActivityRepository activityRepository;
    @Mock UserRepository userRepository;
    ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, activityRepository, userRepository);
    }

    @Test
    void getReviews_returnsPagedReviews() {
        User user = user(1L);
        Activity activity = activity(2L);
        Review review = review(3L, user, activity);
        PageRequest pageable = PageRequest.of(0, 20);
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.findAllByActivityId(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        var response = service.getReviews(2L, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).id()).isEqualTo(3L);
    }

    @Test
    void createReview_increasesReviewCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            setId(saved, 3L);
            return saved;
        });

        service.createReview(1L, 2L, new ReviewRequest(5, "좋아요"));

        assertThat(activity.getReviewCount()).isEqualTo(1);
    }

    @Test
    void createReview_whenRatingInvalid_throwsBadRequest() {
        CustomException exception = assertThrows(CustomException.class,
                () -> service.createReview(1L, 2L, new ReviewRequest(0, "bad")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_INVALID_RATING);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createReview_whenDuplicate_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity(2L)));
        when(reviewRepository.existsByUserIdAndActivityId(1L, 2L)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class,
                () -> service.createReview(1L, 2L, new ReviewRequest(4, "중복")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
    }

    @Test
    void updateReview_updatesOwnedReview() {
        User user = user(1L);
        Activity activity = activity(2L);
        Review review = review(3L, user, activity);
        int beforeReviewCount = activity.getReviewCount();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.of(review));

        var response = service.updateReview(1L, 2L, 3L, new ReviewRequest(3, "수정"));

        assertThat(response.rating()).isEqualTo(3);
        assertThat(activity.getReviewCount()).isEqualTo(beforeReviewCount);
        assertThat(response.reason()).isEqualTo("수정");
    }

    @Test
    void updateReview_whenRatingInvalid_throwsBadRequest() {
        CustomException exception = assertThrows(CustomException.class,
                () -> service.updateReview(1L, 2L, 3L, new ReviewRequest(6, "bad")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_INVALID_RATING);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateReview_whenUserNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.updateReview(1L, 2L, 3L, new ReviewRequest(3, "user")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        verify(activityRepository, never()).findById(any());
    }

    @Test
    void updateReview_whenActivityNotFound_throwsActivityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.updateReview(1L, 2L, 3L, new ReviewRequest(3, "activity")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        verify(reviewRepository, never()).findByIdAndActivityIdAndUserId(any(), any(), any());
    }

    @Test
    void updateReview_whenReviewNotOwned_throwsReviewNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity(2L)));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.updateReview(1L, 2L, 3L, new ReviewRequest(3, "review")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND);
    }

    @Test
    void deleteReview_decreasesReviewCount() {
        User user = user(1L);
        Activity activity = activity(2L);
        activity.increaseReviewCount();
        Review review = review(3L, user, activity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.of(review));

        service.deleteReview(1L, 2L, 3L);

        assertThat(activity.getReviewCount()).isZero();
        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_whenReviewNotOwned_throwsReviewNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity(2L)));
        when(reviewRepository.findByIdAndActivityIdAndUserId(3L, 2L, 1L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> service.deleteReview(1L, 2L, 3L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND);
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
                .region(region).title("활동").description("설명").sourceUrl("https://example.com")
                .recruitEndAt(LocalDateTime.now())
                .price(0).isActive(true).build();
        setId(activity, id);
        return activity;
    }

    private Review review(Long id, User user, Activity activity) {
        Review review = Review.create(user, activity, 5, "좋아요");
        setId(review, id);
        return review;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
