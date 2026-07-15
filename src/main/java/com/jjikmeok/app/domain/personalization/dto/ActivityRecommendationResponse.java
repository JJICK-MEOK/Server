package com.jjikmeok.app.domain.personalization.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "개인화 추천 활동 응답")
public record ActivityRecommendationResponse(
        @Schema(description = "활동 ID", example = "1")
        Long id,

        @Schema(description = "활동 제목", example = "서울 도예 원데이 클래스")
        String title,

        @Schema(description = "활동 썸네일 URL", example = "https://example.com/images/activity.png")
        String thumbnailUrl,

        @Schema(description = "모집 마감일", example = "2026-07-10T18:00:00")
        LocalDateTime recruitEndAt,

        @Schema(description = "로그인한 사용자의 해당 활동 찜 ID. 찜하지 않았으면 null", example = "10", nullable = true)
        Long activityFavoriteId,

        @Schema(description = "DB의 취향 태그 ID 오름차순으로 만든 사용자/활동 int[] 벡터의 코사인 유사도를 0~100점으로 변환한 값. 사용자 또는 활동에 취향 태그가 없으면 null", example = "90", nullable = true)
        Integer personalizationScore,

        @ArraySchema(
                arraySchema = @Schema(description = "활동 태그명 목록"),
                schema = @Schema(description = "활동 태그명", example = "힐링")
        )
        String[] tags
) {
}
