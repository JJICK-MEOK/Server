package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.response.ActivityReviewResponse;
import com.jjikmeok.app.domain.activity.entity.ActivityReview;

public class ActivityReviewConverter {

    private ActivityReviewConverter() {
    }

    public static ActivityReviewResponse toResponse(ActivityReview review) {
        return new ActivityReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getActivity().getId(),
                review.getRating(),
                review.getReason(),
                review.getLikeCount(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
