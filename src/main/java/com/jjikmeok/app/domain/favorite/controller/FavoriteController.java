package com.jjikmeok.app.domain.favorite.controller;

import com.jjikmeok.app.domain.favorite.dto.request.FavoriteRequest;
import com.jjikmeok.app.domain.favorite.dto.response.FavoriteResponse;
import com.jjikmeok.app.domain.favorite.service.FavoriteService;
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

@Tag(name = "Activity Favorite", description = "활동 찜 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "내 활동 찜 목록 조회")
    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getFavorites(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success("활동 찜 목록 조회 성공", favoriteService.getFavorites(userId));
    }

    @Operation(summary = "활동 찜 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> createFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid FavoriteRequest request) {
        return ApiResponse.success("활동 찜 생성 성공", favoriteService.createFavorite(userId, request));
    }

    @Operation(summary = "활동 찜 삭제")
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId) {
        favoriteService.deleteFavorite(userId, activityId);
    }
}
