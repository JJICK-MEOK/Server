package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.dto.request.ActivityReviewRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityReviewResponse;
import com.jjikmeok.app.domain.activity.service.ActivityReviewService;
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

@Tag(name = "Activity Review", description = "활동 후기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities/{activityId}/reviews")
public class ActivityReviewController {

    private final ActivityReviewService reviewService;

    @Operation(summary = "활동 후기 목록 조회")
    @GetMapping
    public ApiResponse<Page<ActivityReviewResponse>> getReviews(
            @PathVariable("activityId") Long activityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success("활동 후기 목록 조회 성공", reviewService.getReviews(activityId, pageable));
    }

    @Operation(summary = "활동 후기 작성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ActivityReviewResponse> createReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @RequestBody @Valid ActivityReviewRequest request) {
        return ApiResponse.success("활동 후기 작성 성공", reviewService.createReview(userId, activityId, request));
    }

    @Operation(summary = "활동 후기 수정")
    @PutMapping("/{reviewId}")
    public ApiResponse<ActivityReviewResponse> updateReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @PathVariable("reviewId") Long reviewId,
            @RequestBody @Valid ActivityReviewRequest request) {
        return ApiResponse.success("활동 후기 수정 성공", reviewService.updateReview(userId, activityId, reviewId, request));
    }

    @Operation(summary = "활동 후기 삭제")
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.deleteReview(userId, activityId, reviewId);
    }
}
