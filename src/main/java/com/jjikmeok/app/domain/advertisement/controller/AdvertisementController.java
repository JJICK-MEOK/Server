package com.jjikmeok.app.domain.advertisement.controller;

import com.jjikmeok.app.domain.advertisement.dto.request.AdvertisementRequest;
import com.jjikmeok.app.domain.advertisement.dto.response.AdvertisementResponse;
import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import com.jjikmeok.app.domain.advertisement.service.AdvertisementService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Advertisement", description = "광고 정보 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/advertisements")
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @Operation(summary = "노출 광고 목록 조회")
    @GetMapping
    public ApiResponse<List<AdvertisementResponse>> getVisibleAdvertisements(
            @RequestParam(value = "position", required = false) AdvertisementPosition position) {
        return ApiResponse.success("광고 목록 조회 성공", advertisementService.getVisibleAdvertisements(position));
    }

    @Operation(summary = "광고 상세 조회")
    @GetMapping("/{advertisementId}")
    public ApiResponse<AdvertisementResponse> getAdvertisement(
            @PathVariable("advertisementId") Long id) {
        return ApiResponse.success("광고 상세 조회 성공", advertisementService.getAdvertisement(id));
    }

    @Operation(summary = "광고 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdvertisementResponse> createAdvertisement(
            @RequestBody @Valid AdvertisementRequest request) {
        return ApiResponse.created("광고 생성 성공", advertisementService.createAdvertisement(request));
    }

    @Operation(summary = "광고 수정")
    @PutMapping("/{advertisementId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdvertisementResponse> updateAdvertisement(
            @PathVariable("advertisementId") Long id,
            @RequestBody @Valid AdvertisementRequest request) {
        return ApiResponse.success("광고 수정 성공", advertisementService.updateAdvertisement(id, request));
    }

    @Operation(summary = "광고 삭제")
    @DeleteMapping("/{advertisementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAdvertisement(@PathVariable("advertisementId") Long id) {
        advertisementService.deleteAdvertisement(id);
    }
}
