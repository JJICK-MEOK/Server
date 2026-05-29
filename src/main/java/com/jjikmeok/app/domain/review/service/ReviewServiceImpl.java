package com.jjikmeok.app.domain.review.service;

import com.jjikmeok.app.domain.review.converter.ReviewConverter;
import com.jjikmeok.app.domain.review.dto.request.ReviewRequest;
import com.jjikmeok.app.domain.review.dto.response.ReviewResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.review.entity.Review;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.review.repository.ReviewRepository;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ReviewResponse> getReviews(Long activityId, Pageable pageable) {
        findActivityOrThrow(activityId);
        return reviewRepository.findAllByActivityId(activityId, pageable)
                .map(ReviewConverter::toResponse);
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, Long activityId, ReviewRequest request) {
        validateRating(request.rating());
        User user = findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(activityId);
        if (reviewRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw new CustomException(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
        }
        try {
            Review review = reviewRepository.save(
                    Review.create(user, activity, request.rating(), request.reason())
            );
            activity.increaseReviewCount();
            return ReviewConverter.toResponse(review);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ACTIVITY_REVIEW_DUPLICATE);
        }
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ReviewRequest request) {
        validateRating(request.rating());
        findUserOrThrow(userId);
        findActivityOrThrow(activityId);
        Review review = findReviewOrThrow(userId, activityId, reviewId);
        review.update(request.rating(), request.reason());
        return ReviewConverter.toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long activityId, Long reviewId) {
        findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(activityId);
        Review review = findReviewOrThrow(userId, activityId, reviewId);
        reviewRepository.delete(review);
        activity.decreaseReviewCount();
    }

    private Review findReviewOrThrow(Long userId, Long activityId, Long reviewId) {
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
