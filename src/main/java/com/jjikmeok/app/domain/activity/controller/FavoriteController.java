package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.dto.request.FavoriteRequest;
import com.jjikmeok.app.domain.activity.dto.response.FavoriteResponse;
import com.jjikmeok.app.domain.activity.service.FavoriteService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "찜 API", description = "활동 찜 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activity-favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "찜한 활동 목록 조회")
    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getFavorites(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "정렬 기준: saved(담은순, 기본값), deadline(마감순)")
            @RequestParam(value = "sort", required = false, defaultValue = "saved") String sort
    ) {
        return ApiResponse.success("찜한 활동 목록 조회 성공", favoriteService.getFavorites(userId, sort));
    }

    @Operation(summary = "찜 추가")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> createFavorite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid FavoriteRequest request
    ) {
        return ApiResponse.success("찜 추가 성공", favoriteService.createFavorite(userId, request));
    }

    @Operation(summary = "찜 삭제")
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable("activityId") Long activityId
    ) {
        favoriteService.deleteFavorite(userId, activityId);
    }
}
