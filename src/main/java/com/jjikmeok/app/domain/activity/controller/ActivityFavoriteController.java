package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.dto.request.ActivityFavoriteRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityFavoriteResponse;
import com.jjikmeok.app.domain.activity.service.ActivityFavoriteService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Activity Favorite", description = "Activity favorite API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activity-favorites")
public class ActivityFavoriteController {

    private final ActivityFavoriteService activityFavoriteService;

    @Operation(summary = "활동 찜 목록 조회")
    @GetMapping
    public ApiResponse<List<ActivityFavoriteResponse>> getFavorites(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success("Activity favorites fetched", activityFavoriteService.getFavorites(userId));
    }

    @Operation(summary = "활동 찜 추가")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ActivityFavoriteResponse> createFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ActivityFavoriteRequest request
    ) {
        return ApiResponse.success("Activity favorite created", activityFavoriteService.createFavorite(userId, request));
    }

    @Operation(summary = "활동 찜 삭제")
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId
    ) {
        activityFavoriteService.deleteFavorite(userId, activityId);
    }
}
