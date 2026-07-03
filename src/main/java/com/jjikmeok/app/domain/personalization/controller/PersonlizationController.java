package com.jjikmeok.app.domain.personalization.controller;

import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.service.PersonlizationService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Personalization", description = "개인화 추천 관련 API")
@RestController
@RequiredArgsConstructor
public class PersonlizationController {

    private final PersonlizationService personlizationService;

    @Operation(
            summary = "개인화 콘텐츠 유형 조회",
            description = "인증된 사용자의 온보딩 태그를 기준으로 가장 적합한 개인화 콘텐츠 유형을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인화 콘텐츠 유형 조회 성공"
            )
    })
    @GetMapping("/api/v1/personalization/users/me/best-type")
    public ApiResponse<PersonalizationResponse> getPersonalizedContent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "개인화 콘텐츠 유형 조회 성공",
                personlizationService.findBestType(userId)
        );
    }

    @Hidden
    @GetMapping("/api/v1/personlization/users/me/best-type")
    public ApiResponse<PersonalizationResponse> getPersonalizedContentLegacy(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return getPersonalizedContent(userId);
    }

    @Operation(
            summary = "개인화 추천 활동 조회",
            description = "로그인한 사용자의 온보딩 태그 기준으로 활동을 추천합니다. 온보딩 태그 매칭 수, 태그명 매칭 수, 좋아요 수 순서로 내림차순 정렬하며 활동 ID, 제목, 썸네일 URL, 모집 마감일, 찜 ID, 태그명 목록을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인화 추천 활동 조회 성공"
            )
    })
    @GetMapping("/api/v1/personalization/users/me/personalization-activities")
    public ApiResponse<List<ActivityRecommendationResponse>> getPersonlizedActivity(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "개인화 추천 활동 조회 성공",
                personlizationService.getRecommendedActivities(userId)
        );
    }

    @Hidden
    @GetMapping({
            "/api/v1/personlization/users/me/personlization-activities",
            "/api/v1/personlization/users/me/personalization-activities",
            "/api/v1/personalization/users/me/personlization-activities"
    })
    public ApiResponse<List<ActivityRecommendationResponse>> getPersonlizedActivityLegacy(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return getPersonlizedActivity(userId);
    }
}
