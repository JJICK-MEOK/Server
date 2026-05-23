package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.converter.ActivityReviewConverter;
import com.jjikmeok.app.domain.activity.dto.request.ActivityReviewRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityReviewResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityReview;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityReviewRepository;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityReviewServiceImpl implements ActivityReviewService {

    private final ActivityReviewRepository reviewRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ActivityReviewResponse> getReviews(Long activityId, Pageable pageable) {
        findActivityOrThrow(activityId);
        return reviewRepository.findAllByActivityId(activityId, pageable)
                .map(ActivityReviewConverter::toResponse);
    }

    @Override
    @Transactional
    public ActivityReviewResponse createReview(Long userId, Long activityId, ActivityReviewRequest request) {
        validateRating(request.rating());
        User user = findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(activityId);
        if (reviewRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw new CustomException(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
        }
        try {
            ActivityReview review = reviewRepository.save(
                    ActivityReview.create(user, activity, request.rating(), request.reason())
            );
            activity.increaseReviewCount();
            return ActivityReviewConverter.toResponse(review);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
        }
    }

    @Override
    @Transactional
    public ActivityReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ActivityReviewRequest request) {
        validateRating(request.rating());
        findUserOrThrow(userId);
        findActivityOrThrow(activityId);
        ActivityReview review = findReviewOrThrow(userId, activityId, reviewId);
        review.update(request.rating(), request.reason());
        return ActivityReviewConverter.toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long activityId, Long reviewId) {
        findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(activityId);
        ActivityReview review = findReviewOrThrow(userId, activityId, reviewId);
        reviewRepository.delete(review);
        activity.decreaseReviewCount();
    }

    private ActivityReview findReviewOrThrow(Long userId, Long activityId, Long reviewId) {
        return reviewRepository.findByIdAndActivityIdAndUserId(reviewId, activityId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_REVIEW_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Activity findActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new CustomException(ErrorCode.ACTIVITY_REVIEW_INVALID_RATING);
        }
    }
}
