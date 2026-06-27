package com.jjikmeok.app.domain.review.converter;

import com.jjikmeok.app.domain.review.dto.response.ReviewResponse;
import com.jjikmeok.app.domain.review.entity.Review;

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
