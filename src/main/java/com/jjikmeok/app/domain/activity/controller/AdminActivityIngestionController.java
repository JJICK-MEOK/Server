package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.service.AdminActivityIngestionService;
import com.jjikmeok.app.domain.discovery.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.sync.dto.ActivitySyncResponse;
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

@Tag(name = "Admin Activity Ingestion API", description = "Admin activity ingestion API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/activities/sources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityIngestionController {

    private final AdminActivityIngestionService adminActivityIngestionService;

    @Operation(summary = "Sync all public activity sources")
    @PostMapping("/public/sync")
    public ApiResponse<String> syncAllPublicSources() {
        adminActivityIngestionService.syncAllPublicSources();
        return ApiResponse.success("Public activity sync started", "Check server logs for details.");
    }

    @Operation(summary = "Sync one public activity source")
    @PostMapping("/public/{sourceType}/sync")
    public ApiResponse<ActivitySyncResponse> syncPublicSource(
            @PathVariable("sourceType") SourceType sourceType,
            @RequestParam(required = false) Integer maxPages
    ) {
        return ApiResponse.success(
                "Public activity source synced",
                adminActivityIngestionService.syncPublicSource(sourceType, maxPages)
        );
    }

    @Operation(summary = "Collect discovery activity candidates")
    @PostMapping("/discovery/collect")
    public ApiResponse<List<DiscoverySheetRowDto>> collectDiscoveryActivities(
            @RequestParam(required = false) Integer keywordLimit,
            @RequestParam(required = false) Integer resultLimit
    ) {
        return ApiResponse.success(
                "Discovery activity candidates collected",
                adminActivityIngestionService.collectDiscoveryActivities(keywordLimit, resultLimit)
        );
    }

    @Operation(summary = "Publish ready discovery activities")
    @PostMapping("/discovery/publish")
    public ApiResponse<Integer> publishDiscoveryActivities() {
        return ApiResponse.success(
                "Discovery activities published",
                adminActivityIngestionService.publishDiscoveryActivities()
        );
    }
}
