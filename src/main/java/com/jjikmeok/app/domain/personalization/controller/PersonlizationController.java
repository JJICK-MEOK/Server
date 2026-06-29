package com.jjikmeok.app.domain.personalization.controller;

import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.service.PersonlizationService;
import com.jjikmeok.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Personalization", description = "개인화 추천 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/personalization")
public class PersonlizationController {

    private final PersonlizationService personlizationService;

    @Operation(
            summary = "개인화 콘텐츠 유형 조회",
            description = "인증된 사용자를 기준으로 가장 적합한 개인화 콘텐츠 유형을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "개인화 콘텐츠 유형 조회 성공"
            )
    })

    @GetMapping("/users/me/best-type")
    public ApiResponse<PersonalizationResponse> getPersonalizedContent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "개인화 콘텐츠 유형 조회 성공",
                personlizationService.findBestType(userId)
        );
    }

    @Operation(
            summary = "개인화 활동 조회",
            description = "인증된 사용자를 기준으로 가장 적합한 활동을 조회합니다."
    )
    @GetMapping("/users/me/personlization-activities")
    public ApiResponse<List<ActivityRecommendationResponse>> getPersonlizedActivity(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "개인화된 추천 활동 조회 성공",
                personlizationService.getRecommendedActivities(userId)
        );
    }
}
