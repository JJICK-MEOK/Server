package com.jjikmeok.app.domain.activity.controller;

import com.jjikmeok.app.domain.activity.dto.request.ActivityRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.dto.response.ActivitySummaryResponse;
import com.jjikmeok.app.domain.activity.service.ActivityService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Activity", description = "활동 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "활동 목록 조회", description = "노출 중인 활동 목록을 조회합니다. regionId를 전달하면 해당 지역 활동만 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON_500", value = "{\"code\":\"COMMON_500\",\"message\":\"서버 오류가 발생했습니다. 관리자에게 문의해 주세요.\"}")))
    })
    @GetMapping
    public ApiResponse<List<ActivitySummaryResponse>> getActivities(
            @Parameter(description = "조회할 지역 ID. 생략하면 전체 활동을 조회합니다.", example = "1")
            @RequestParam(value = "regionId", required = false) Long regionId) {
        return ApiResponse.success("활동 목록 조회 성공", activityService.getActivities(regionId));
    }

    @Operation(summary = "활동 상세 조회", description = "활동 ID로 특정 활동의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "ACTIVITY_404", value = "{\"code\":\"ACTIVITY_404\",\"message\":\"해당 활동 정보를 찾을 수 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON_500", value = "{\"code\":\"COMMON_500\",\"message\":\"서버 오류가 발생했습니다. 관리자에게 문의해 주세요.\"}")))
    })
    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> getActivity(
            @Parameter(description = "조회할 활동 ID", example = "1")
            @PathVariable("activityId") Long id) {
        return ApiResponse.success("활동 조회 성공", activityService.getActivity(id));
    }

    @Operation(summary = "활동 생성 (관리자)", description = "새로운 활동 정보를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "활동 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "활동 입력값 오류",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "ACTIVITY_400_RECRUIT_PERIOD", value = "{\"code\":\"ACTIVITY_400_RECRUIT_PERIOD\",\"message\":\"모집 시작일은 모집 마감일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_ACTIVITY_PERIOD", value = "{\"code\":\"ACTIVITY_400_ACTIVITY_PERIOD\",\"message\":\"활동 시작일은 활동 종료일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_SCHEDULE_ORDER", value = "{\"code\":\"ACTIVITY_400_SCHEDULE_ORDER\",\"message\":\"모집 마감일은 활동 시작일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_URI", value = "{\"code\":\"ACTIVITY_400_URI\",\"message\":\"활동 URI 형식이 올바르지 않습니다.\"}")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityDetailResponse> createActivity(
            @RequestBody @Valid ActivityRequest request) {
        return ApiResponse.success("활동 생성 성공", activityService.createActivity(request));
    }

    @Operation(summary = "활동 수정 (관리자)", description = "특정 활동 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "활동 입력값 오류",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "ACTIVITY_400_RECRUIT_PERIOD", value = "{\"code\":\"ACTIVITY_400_RECRUIT_PERIOD\",\"message\":\"모집 시작일은 모집 마감일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_ACTIVITY_PERIOD", value = "{\"code\":\"ACTIVITY_400_ACTIVITY_PERIOD\",\"message\":\"활동 시작일은 활동 종료일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_SCHEDULE_ORDER", value = "{\"code\":\"ACTIVITY_400_SCHEDULE_ORDER\",\"message\":\"모집 마감일은 활동 시작일보다 늦을 수 없습니다.\"}"),
                                    @ExampleObject(name = "ACTIVITY_400_URI", value = "{\"code\":\"ACTIVITY_400_URI\",\"message\":\"활동 URI 형식이 올바르지 않습니다.\"}")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "활동 또는 지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "ACTIVITY_404", value = "{\"code\":\"ACTIVITY_404\",\"message\":\"해당 활동 정보를 찾을 수 없습니다.\"}"),
                                    @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}")
                            }))
    })
    @PutMapping("/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityDetailResponse> updateActivity(
            @Parameter(description = "수정할 활동 ID", example = "1")
            @PathVariable("activityId") Long id,
            @RequestBody @Valid ActivityRequest request) {
        return ApiResponse.success("활동 수정 성공", activityService.updateActivity(id, request));
    }

    @Operation(summary = "활동 삭제 (관리자)", description = "특정 활동 정보를 비활성화 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "활동 삭제 성공", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "ACTIVITY_404", value = "{\"code\":\"ACTIVITY_404\",\"message\":\"해당 활동 정보를 찾을 수 없습니다.\"}")))
    })
    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteActivity(
            @Parameter(description = "삭제할 활동 ID", example = "1")
            @PathVariable("activityId") Long id) {
        activityService.deleteActivity(id);
    }
}
