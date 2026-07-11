package com.jjikmeok.app.domain.page.controller;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCurationDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityFavoritePageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityHomePageResponse;
import com.jjikmeok.app.domain.page.service.PageService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Page", description = "페이지 화면 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pages")
public class PageController {

    private final PageService pageService;

    @Operation(summary = "홈 화면 조회")
    @GetMapping("/home")
    public ApiResponse<ActivityHomePageResponse> getHomePage(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success("홈 페이지 조회 성공", pageService.getHomePage(userId));
    }

    @Operation(summary = "카테고리 화면 조회")
    @GetMapping("/category")
    public ApiResponse<ActivityCategoryPageResponse> getCategoryPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "활동 유형", example = "PROGRAM")
            @RequestParam(value = "type", required = false) ActivityType type,
            @Parameter(description = "활동 카테고리", example = "CRAFT")
            @RequestParam(value = "category", required = false) ActivityCategory category,
            @Parameter(description = "정렬 방식", example = "recommended")
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "조회 개수", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success("카테고리 페이지 조회 성공",
                pageService.getCategoryPage(userId, type, category, sort, limit));
    }

    @Operation(summary = "맞춤 화면 조회")
    @GetMapping("/custom")
    public ApiResponse<ActivityCustomPageResponse> getCustomPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회 개수", example = "10")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success("맞춤 페이지 조회 성공", pageService.getCustomPage(userId, limit));
    }

    @Operation(summary = "찜한 활동 조회")
    @GetMapping("/favorites")
    public ApiResponse<ActivityFavoritePageResponse> getFavoritePage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "정렬 방식", example = "saved")
            @RequestParam(value = "sort", required = false, defaultValue = "saved") String sort
    ) {
        return ApiResponse.success("찜한 활동 페이지 조회 성공", pageService.getFavoritePage(userId, sort));
    }

    @Operation(summary = "상세 화면 조회")
    @GetMapping("/detail/{activityId}")
    public ApiResponse<ActivityDetailPageResponse> getDetailPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "활동 ID", example = "1")
            @PathVariable Long activityId
    ) {
        return ApiResponse.success("상세 페이지 조회 성공", pageService.getDetailPage(userId, activityId));
    }

    @Operation(summary = "홈 큐레이션 상세 조회")
    @GetMapping("/home/curations/{curationKey}")
    public ApiResponse<ActivityCurationDetailPageResponse> getHomeCurationDetailPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "큐레이션 키", example = "SOLO_CULTURE")
            @PathVariable String curationKey
    ) {
        return ApiResponse.success("홈 큐레이션 상세 조회 성공",
                pageService.getHomeCurationDetailPage(userId, curationKey));
    }
}
