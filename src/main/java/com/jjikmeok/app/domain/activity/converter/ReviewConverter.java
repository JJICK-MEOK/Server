package com.jjikmeok.app.domain.activity.converter;

import com.jjikmeok.app.domain.activity.dto.response.ReviewResponse;
import com.jjikmeok.app.domain.activity.entity.Review;

public class ReviewConverter {

    private ReviewConverter() {
    }

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
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
