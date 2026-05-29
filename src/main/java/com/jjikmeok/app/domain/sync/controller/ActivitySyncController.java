package com.jjikmeok.app.domain.sync.controller;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.sync.dto.ActivitySyncResponse;
import com.jjikmeok.app.domain.sync.service.ActivitySyncService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity Sync", description = "관리자 외부 활동 동기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/activity-sync")
@PreAuthorize("hasRole('ADMIN')")
public class ActivitySyncController {

    private final ActivitySyncService activitySyncService;

    @Operation(summary = "🔄 [마스터 대량 동기화] 5대 오픈 API 일괄 연동 구동", description = "자바 1차 가드로 전수 필드를 검증하여 유실된 부실 데이터만 선별적으로 AI 스크래핑 파이프라인에 주입해 요금을 대폭 아낍니다.")
    @PostMapping("/sync-all-sources")
    public ApiResponse<String> syncAllSources() {
        activitySyncService.syncAllSources();
        return ApiResponse.success("비용 최적화 5대 오픈 API 마스터 파이프라인 가동 성공", "서버 백그라운드 콘솔 로그를 확인하세요.");
    }

    @Operation(summary = "Tour API 활동 동기화")
    @PostMapping("/tour-api")
    public ApiResponse<ActivitySyncResponse> syncTourApi(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.TOUR_API, maxPages);
    }

    @Operation(summary = "KOPIS 활동 동기화")
    @PostMapping("/kopis")
    public ApiResponse<ActivitySyncResponse> syncKopis(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.KOPIS, maxPages);
    }

    @Operation(summary = "전시 API 활동 동기화")
    @PostMapping("/exhibition")
    public ApiResponse<ActivitySyncResponse> syncExhibition(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.EXHIBITION, maxPages);
    }

    @Operation(summary = "1365 봉사 활동 동기화")
    @PostMapping("/volunteer-1365")
    public ApiResponse<ActivitySyncResponse> syncVolunteer1365(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.VOLUNTEER_1365, maxPages);
    }

    @Operation(summary = "청년 콘텐츠 동기화")
    @PostMapping("/youth-content")
    public ApiResponse<ActivitySyncResponse> syncYouthContent(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.YOUTH_CONTENT, maxPages);
    }

    @Operation(summary = "서울 문화행사 동기화")
    @PostMapping("/seoul-culture")
    public ApiResponse<ActivitySyncResponse> syncSeoulCulture(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.SEOUL_CULTURE, maxPages);
    }

    @Operation(summary = "서울 공공예약 동기화")
    @PostMapping("/seoul-reservation")
    public ApiResponse<ActivitySyncResponse> syncSeoulReservation(@RequestParam(required = false) Integer maxPages) {
        return sync(SourceType.SEOUL_RESERVATION, maxPages);
    }

    private ApiResponse<ActivitySyncResponse> sync(SourceType sourceType, Integer maxPages) {
        return ApiResponse.success("활동 동기화 성공", activitySyncService.sync(sourceType, null, maxPages));
    }
}