package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ReviewRequest;
import com.jjikmeok.app.domain.activity.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    Page<ReviewResponse> getReviews(Long activityId, Pageable pageable);
    ReviewResponse createReview(Long userId, Long activityId, ReviewRequest request);
    ReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ReviewRequest request);
    void deleteReview(Long userId, Long activityId, Long reviewId);
}
