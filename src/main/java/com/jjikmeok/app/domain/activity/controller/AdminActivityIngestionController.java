package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.enums.PublicActivitySourceType;
import com.jjikmeok.app.domain.activity.privateactivity.dto.response.DiscoverySheetRowDto;
import com.jjikmeok.app.domain.activity.publicactivity.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.activity.service.AdminActivityIngestionService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Activity Ingestion Admin API", description = "Admin APIs for public and discovery activity ingestion")
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
        return ApiResponse.success("공공 활동 동기화를 시작했습니다.", "상세 내용은 서버 로그를 확인하세요.");
    }

    @Operation(summary = "Sync a public activity source")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Public activity sync completed",
                    content = @Content(schema = @Schema(implementation = ActivitySyncResponseEnvelope.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid source type or request parameter",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON_400_PARAMETER",
                                    value = "{\"code\":\"COMMON_400_PARAMETER\",\"message\":\"요청 파라미터가 올바르지 않습니다.\"}"
                            )
                    )
            )
    })
    @PostMapping("/public/{sourceType}/sync")
    public ApiResponse<ActivitySyncResponse> syncPublicSource(
            @Parameter(
                    description = "Public source type",
                    schema = @Schema(implementation = PublicActivitySourceType.class)
            )
            @PathVariable("sourceType") PublicActivitySourceType sourceType,
            @RequestParam(required = false) Integer maxPages
    ) {
        return ApiResponse.success(
                "공공 활동 소스 동기화를 완료했습니다.",
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
                "디스커버리 활동 후보 수집을 완료했습니다.",
                adminActivityIngestionService.collectDiscoveryActivities(keywordLimit, resultLimit)
        );
    }

    @Operation(summary = "Publish queued discovery activities")
    @PostMapping("/discovery/publish")
    public ApiResponse<Integer> publishDiscoveryActivities() {
        return ApiResponse.success(
                "디스커버리 활동 발행을 완료했습니다.",
                adminActivityIngestionService.publishDiscoveryActivities()
        );
    }

    @Operation(summary = "Publish queued public activities")
    @PostMapping("/public/publish")
    public ApiResponse<Integer> publishPublicActivities() {
        return ApiResponse.success(
                "공공 활동 발행을 완료했습니다.",
                adminActivityIngestionService.publishPublicActivities()
        );
    }

    @Schema(name = "ActivitySyncResponseEnvelope", description = "Common response envelope for public activity sync")
    private record ActivitySyncResponseEnvelope(
            @Schema(example = "200") String code,
            @Schema(example = "공공 활동 소스 동기화를 완료했습니다.") String message,
            ActivitySyncResponse data
    ) {
    }
}
