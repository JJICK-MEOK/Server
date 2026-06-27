package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.service.AdminActivityIngestionService;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "활동 수집 관리자 API", description = "공공/민간/디스커버리 활동 수집 관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/activities/sources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityIngestionController {

    private final AdminActivityIngestionService adminActivityIngestionService;

    @Operation(summary = "공공 활동 소스 전체 동기화")
    @PostMapping("/public/sync")
    public ApiResponse<String> syncAllPublicSources() {
        adminActivityIngestionService.syncAllPublicSources();
        return ApiResponse.success("공공 활동 동기화를 시작했습니다.", "상세 내용은 서버 로그를 확인하세요.");
    }

    @Operation(summary = "공공 활동 소스 개별 동기화")
    @PostMapping("/public/{sourceType}/sync")
    public ApiResponse<ActivitySyncResponse> syncPublicSource(
            @PathVariable("sourceType") SourceType sourceType,
            @RequestParam(required = false) Integer maxPages
    ) {
        return ApiResponse.success(
                "공공 활동 소스 동기화를 완료했습니다.",
                adminActivityIngestionService.syncPublicSource(sourceType, maxPages)
        );
    }

    @Operation(summary = "디스커버리 활동 후보 수집")
    @PostMapping("/discovery/collect")
    public ApiResponse<List<DiscoverySheetRowDto>> collectDiscoveryActivities(
            @RequestParam(required = false) Integer keywordLimit,
            @RequestParam(required = false) Integer resultLimit
    ) {
        return ApiResponse.success(
                "디스커버리 활동 후보 수집을 완료했습니다.",
                adminActivityIngestionService.collectDiscoveryActivities(keywordLimit, resultLimit)
        );
    }

    @Operation(summary = "발행 대기 디스커버리 활동 발행")
    @PostMapping("/discovery/publish")
    public ApiResponse<Integer> publishDiscoveryActivities() {
        return ApiResponse.success(
                "디스커버리 활동 발행을 완료했습니다.",
                adminActivityIngestionService.publishDiscoveryActivities()
        );
    }
}
