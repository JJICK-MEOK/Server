package com.jjikmeok.app.domain.region.controller;

import com.jjikmeok.app.domain.region.dto.request.RegionRequest;
import com.jjikmeok.app.domain.region.dto.response.RegionResponse;
import com.jjikmeok.app.domain.region.service.RegionService;
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

@Tag(name = "Region", description = "지역 정보 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "지역 목록 조회", description = "parentId가 없으면 최상위 지역을, 있으면 해당 지역의 하위 지역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON_500", value = "{\"code\":\"COMMON_500\",\"message\":\"서버 오류가 발생했습니다. 관리자에게 문의해 주세요.\"}")))
    })
    @GetMapping
    public ApiResponse<List<RegionResponse>> getRegions(
            @Parameter(description = "상위 지역 ID. 최상위 지역 조회 시 생략합니다.", example = "1")
            @RequestParam(value = "parentId", required = false) Long parentId) {

        List<RegionResponse> response = regionService.getRegions(parentId);
        return ApiResponse.success("지역 목록 조회 성공", response);
    }

    @Operation(summary = "단일 지역 상세 조회", description = "지역 ID로 특정 지역의 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}")))
    })
    @GetMapping("/{regionId}")
    public ApiResponse<RegionResponse> getRegionById(
            @Parameter(description = "조회할 지역 ID", example = "1")
            @PathVariable("regionId") Long id) {

        RegionResponse response = regionService.getRegionById(id);
        return ApiResponse.success("지역 상세 조회 성공", response);
    }

    @Operation(summary = "지역 생성 (관리자)", description = "새로운 지역 정보를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "지역 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지역 계층 입력값 오류",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "REGION_400_PARENT_REQUIRED", value = "{\"code\":\"REGION_400_PARENT_REQUIRED\",\"message\":\"하위 지역(DISTRICT)은 상위 지역이 필요합니다.\"}"),
                                    @ExampleObject(name = "REGION_400_PARENT_NOT_ALLOWED", value = "{\"code\":\"REGION_400_PARENT_NOT_ALLOWED\",\"message\":\"상위 지역(PROVINCE)은 parent를 가질 수 없습니다.\"}"),
                                    @ExampleObject(name = "REGION_400_PARENT_DEPTH", value = "{\"code\":\"REGION_400_PARENT_DEPTH\",\"message\":\"하위 지역의 상위는 PROVINCE만 가능합니다.\"}")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상위 지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_404_PARENT", value = "{\"code\":\"REGION_404_PARENT\",\"message\":\"상위 지역 정보를 찾을 수 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 상위 지역 내 지역명 중복",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_409_DUPLICATE_NAME", value = "{\"code\":\"REGION_409_DUPLICATE_NAME\",\"message\":\"같은 상위 지역 내에 동일한 지역명이 이미 존재합니다.\"}")))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RegionResponse> createRegion(
            @RequestBody @Valid RegionRequest request) {

        RegionResponse response = regionService.createRegion(request);
        return ApiResponse.created("지역 생성 성공", response);
    }

    @Operation(summary = "지역 수정 (관리자)", description = "특정 지역 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지역 계층 입력값 오류",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "REGION_400_SELF_PARENT", value = "{\"code\":\"REGION_400_SELF_PARENT\",\"message\":\"자기 자신을 상위 지역으로 지정할 수 없습니다.\"}"),
                                    @ExampleObject(name = "REGION_400_PARENT_REQUIRED", value = "{\"code\":\"REGION_400_PARENT_REQUIRED\",\"message\":\"하위 지역(DISTRICT)은 상위 지역이 필요합니다.\"}"),
                                    @ExampleObject(name = "REGION_400_PARENT_NOT_ALLOWED", value = "{\"code\":\"REGION_400_PARENT_NOT_ALLOWED\",\"message\":\"상위 지역(PROVINCE)은 parent를 가질 수 없습니다.\"}"),
                                    @ExampleObject(name = "REGION_400_PARENT_DEPTH", value = "{\"code\":\"REGION_400_PARENT_DEPTH\",\"message\":\"하위 지역의 상위는 PROVINCE만 가능합니다.\"}")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역 또는 상위 지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}"),
                                    @ExampleObject(name = "REGION_404_PARENT", value = "{\"code\":\"REGION_404_PARENT\",\"message\":\"상위 지역 정보를 찾을 수 없습니다.\"}")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 상위 지역 내 지역명 중복",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_409_DUPLICATE_NAME", value = "{\"code\":\"REGION_409_DUPLICATE_NAME\",\"message\":\"같은 상위 지역 내에 동일한 지역명이 이미 존재합니다.\"}")))
    })
    @PutMapping("/{regionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RegionResponse> updateRegion(
            @Parameter(description = "수정할 지역 ID", example = "1")
            @PathVariable("regionId") Long id,
            @RequestBody @Valid RegionRequest request) {

        RegionResponse response = regionService.updateRegion(id, request);
        return ApiResponse.success("지역 수정 성공", response);
    }

    @Operation(summary = "지역 삭제 (관리자)", description = "특정 지역 정보를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "지역 삭제 성공", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "AUTH_403", value = "{\"code\":\"AUTH_403\",\"message\":\"해당 API에 대한 접근 권한이 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지역을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_404", value = "{\"code\":\"REGION_404\",\"message\":\"해당 지역 정보를 찾을 수 없습니다.\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "사용 중인 지역이라 삭제할 수 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "REGION_409_IN_USE", value = "{\"code\":\"REGION_409_IN_USE\",\"message\":\"사용 중인 지역이라 삭제할 수 없습니다.\"}")))
    })
    @DeleteMapping("/{regionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRegion(
            @Parameter(description = "삭제할 지역 ID", example = "1")
            @PathVariable("regionId") Long id) {

        regionService.deleteRegion(id);
    }
}
