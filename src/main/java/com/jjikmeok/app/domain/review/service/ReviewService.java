package com.jjikmeok.app.domain.review.service;

import com.jjikmeok.app.domain.review.dto.request.ReviewRequest;
import com.jjikmeok.app.domain.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Page<ReviewResponse> getReviews(Long activityId, Pageable pageable);
    ReviewResponse createReview(Long userId, Long activityId, ReviewRequest request);
    ReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ReviewRequest request);
    void deleteReview(Long userId, Long activityId, Long reviewId);
}
