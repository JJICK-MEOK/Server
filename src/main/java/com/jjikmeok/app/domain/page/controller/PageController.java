package com.jjikmeok.app.domain.page.controller;

import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.page.dto.response.ActivityCategoryPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityCustomPageResponse;
import com.jjikmeok.app.domain.page.dto.response.ActivityDetailPageResponse;
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

@Tag(name = "Page", description = "프론트 화면 단위 활동 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pages")
public class PageController {

    private final PageService pageService;

    @Operation(summary = "홈 화면 활동 데이터 조회", description = "홈 화면의 사용자 정보, 추천 활동, 마감 임박 활동을 조회합니다.")
    @GetMapping("/home")
    public ApiResponse<ActivityHomePageResponse> getHomePage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "섹션별 최대 활동 개수", example = "10")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success("홈 페이지 조회 성공", pageService.getHomePage(userId, limit));
    }

    @Operation(summary = "카테고리 화면 활동 데이터 조회", description = "활동 유형과 활동 카테고리 필터에 맞는 목록 화면 데이터를 조회합니다.")
    @GetMapping("/category")
    public ApiResponse<ActivityCategoryPageResponse> getCategoryPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "활동 유형", example = "\"PROGRAM\"")
            @RequestParam(value = "type", required = false) ActivityType type,
            @Parameter(description = "활동 카테고리", example = "\"CRAFT\"")
            @RequestParam(value = "category", required = false) ActivityCategory category,
            @Parameter(description = "정렬. recommended, deadline, popular", example = "recommended")
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "최대 활동 개수", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success("카테고리 페이지 조회 성공",
                pageService.getCategoryPage(userId, type, category, sort, limit));
    }

    @Operation(summary = "맞춤 화면 활동 데이터 조회", description = "사용자 온보딩 취향과 지역을 기준으로 맞춤 추천 화면 데이터를 조회합니다.")
    @GetMapping("/custom")
    public ApiResponse<ActivityCustomPageResponse> getCustomPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "최대 활동 개수", example = "10")
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success("맞춤 페이지 조회 성공", pageService.getCustomPage(userId, limit));
    }

    @Operation(summary = "상세 화면 활동 데이터 조회", description = "상세 화면에 표시할 활동 정보, 이미지, 찜 여부를 조회합니다.")
    @GetMapping("/detail/{activityId}")
    public ApiResponse<ActivityDetailPageResponse> getDetailPage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "활동 ID", example = "1")
            @PathVariable Long activityId
    ) {
        return ApiResponse.success("상세 페이지 조회 성공", pageService.getDetailPage(userId, activityId));
    }
}
