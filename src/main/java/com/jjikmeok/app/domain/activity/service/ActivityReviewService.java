package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityReviewRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityReviewService {
    Page<ActivityReviewResponse> getReviews(Long activityId, Pageable pageable);
    ActivityReviewResponse createReview(Long userId, Long activityId, ActivityReviewRequest request);
    ActivityReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ActivityReviewRequest request);
    void deleteReview(Long userId, Long activityId, Long reviewId);
}
