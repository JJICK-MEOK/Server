package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.dto.request.ActivityReviewRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityReviewResponse;

import java.util.List;

public interface ActivityReviewService {
    List<ActivityReviewResponse> getReviews(Long activityId);
    ActivityReviewResponse createReview(Long userId, Long activityId, ActivityReviewRequest request);
    ActivityReviewResponse updateReview(Long userId, Long activityId, Long reviewId, ActivityReviewRequest request);
    void deleteReview(Long userId, Long activityId, Long reviewId);
}
