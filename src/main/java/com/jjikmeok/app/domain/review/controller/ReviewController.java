package com.jjikmeok.app.domain.review.controller;

import com.jjikmeok.app.domain.review.dto.request.ReviewRequest;
import com.jjikmeok.app.domain.review.dto.response.ReviewResponse;
import com.jjikmeok.app.domain.review.service.ReviewService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰 API", description = "활동 리뷰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities/{activityId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 목록 조회")
    @GetMapping
    public ApiResponse<Page<ReviewResponse>> getReviews(
            @PathVariable("activityId") Long activityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("리뷰 목록 조회 성공", reviewService.getReviews(activityId, pageable));
    }

    @Operation(summary = "리뷰 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @RequestBody @Valid ReviewRequest request
    ) {
        return ApiResponse.success("리뷰 생성 성공", reviewService.createReview(userId, activityId, request));
    }

    @Operation(summary = "리뷰 수정")
    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody @Valid ReviewRequest request
    ) {
        return ApiResponse.success(
                "리뷰 수정 성공",
                reviewService.updateReview(userId, activityId, reviewId, request)
        );
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @PathVariable("reviewId") Long reviewId
    ) {
        reviewService.deleteReview(userId, activityId, reviewId);
    }
}
