package com.jjikmeok.app.domain.image.controller;

import com.jjikmeok.app.domain.image.dto.request.ImageRequest;
import com.jjikmeok.app.domain.image.dto.response.ImageResponse;
import com.jjikmeok.app.domain.image.service.ImageService;
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

@Tag(name = "이미지 API", description = "활동 이미지 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities/{activityId}/images")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "이미지 목록 조회")
    @GetMapping
    public ApiResponse<List<ImageResponse>> getImages(
            @PathVariable("activityId") Long activityId) {
        return ApiResponse.success("이미지 목록 조회 성공", imageService.getImages(activityId));
    }

    @Operation(summary = "이미지 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImageResponse> createImage(
            @PathVariable("activityId") Long activityId,
            @RequestBody @Valid ImageRequest request) {
        return ApiResponse.created("이미지 생성 성공", imageService.createImage(activityId, request));
    }

    @Operation(summary = "이미지 수정")
    @PutMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImageResponse> updateImage(
            @PathVariable("activityId") Long activityId,
            @PathVariable("imageId") Long imageId,
            @RequestBody @Valid ImageRequest request) {
        return ApiResponse.success(
                "이미지 수정 성공",
                imageService.updateImage(activityId, imageId, request)
        );
    }

    @Operation(summary = "이미지 삭제")
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteImage(
            @PathVariable("activityId") Long activityId,
            @PathVariable("imageId") Long imageId) {
        imageService.deleteImage(activityId, imageId);
    }
}
