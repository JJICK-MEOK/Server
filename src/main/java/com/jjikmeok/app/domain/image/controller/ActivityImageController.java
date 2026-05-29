package com.jjikmeok.app.domain.image.controller;

import com.jjikmeok.app.domain.image.dto.request.ActivityImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ActivityImageResponse;
import com.jjikmeok.app.domain.image.service.ActivityImageService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Activity Image", description = "활동 이미지 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities/{activityId}/images")
public class ActivityImageController {

    private final ActivityImageService activityImageService;

    @Operation(summary = "활동 이미지 목록 조회")
    @GetMapping
    public ApiResponse<List<ActivityImageResponse>> getActivityImages(
            @PathVariable("activityId") Long activityId) {
        return ApiResponse.success("활동 이미지 목록 조회 성공", activityImageService.getActivityImages(activityId));
    }

    @Operation(summary = "활동 이미지 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityImageResponse> createActivityImage(
            @PathVariable("activityId") Long activityId,
            @RequestBody @Valid ActivityImageRequest request) {
        return ApiResponse.success("활동 이미지 생성 성공", activityImageService.createActivityImage(activityId, request));
    }

    @Operation(summary = "활동 이미지 수정")
    @PutMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityImageResponse> updateActivityImage(
            @PathVariable("activityId") Long activityId,
            @PathVariable("imageId") Long imageId,
            @RequestBody @Valid ActivityImageRequest request) {
        return ApiResponse.success(
                "활동 이미지 수정 성공",
                activityImageService.updateActivityImage(activityId, imageId, request)
        );
    }

    @Operation(summary = "활동 이미지 삭제")
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteActivityImage(
            @PathVariable("activityId") Long activityId,
            @PathVariable("imageId") Long imageId) {
        activityImageService.deleteActivityImage(activityId, imageId);
    }
}
